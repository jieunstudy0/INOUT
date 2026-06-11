import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getInquiryDetail, createComment, deleteComment, updateComment } from '../api/inquiryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

function parseToken() {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch { return null; }
}

function InquiryStatusBadge({ isRead }) {
  if (isRead) return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">확인 완료</span>;
  return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-600">답변 대기</span>;
}

export default function InquiryDetailAdmPage() {
  const { inquiryId } = useParams();
  const navigate = useNavigate();
  
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [commentContent, setCommentContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [replyTo, setReplyTo] = useState(null); 
  
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editContent, setEditContent] = useState('');

  const currentUser = parseToken();

  const checkIsMine = (comment) => {
    if (!currentUser) return false;
    if (currentUser.id && String(currentUser.id) === String(comment.authorId)) return true;
    if (currentUser.userId && String(currentUser.userId) === String(comment.authorId)) return true;
    if (currentUser.sub && currentUser.sub === comment.authorEmail) return true;
    if (currentUser.email && currentUser.email === comment.authorEmail) return true;
    
    return false;
  };

  const loadDetail = useCallback(() => {
    setLoading(true);
    getInquiryDetail(inquiryId)
      .then(setDetail)
      .catch(() => {
        Toast.error('상세 내역을 불러오지 못했습니다.');
        navigate(-1);
      })
      .finally(() => setLoading(false));
  }, [inquiryId, navigate]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  const handleCommentSubmit = async (e) => {
    e.preventDefault();
    if (!commentContent.trim()) return;
    setSubmitting(true);
    try {
      await createComment(inquiryId, { 
        content: commentContent.trim(),
        parentId: replyTo ? replyTo.id : null 
      });
      setCommentContent('');
      setReplyTo(null); 
      Toast.success(replyTo ? '답댓글이 등록되었습니다.' : '답변이 등록되었습니다.');
      loadDetail(); 
    } catch {} finally {
      setSubmitting(false);
    }
  };

  const handleEditStart = (comment) => {
    setEditingCommentId(comment.id);
    setEditContent(comment.content);
    setReplyTo(null); 
  };

  const handleEditCancel = () => {
    setEditingCommentId(null);
    setEditContent('');
  };

  const handleEditSubmit = async (commentId) => {
    if (!editContent.trim()) return;
    try {
      await updateComment(inquiryId, commentId, editContent.trim());
      Toast.success('답변이 수정되었습니다.');
      setEditingCommentId(null);
      setEditContent('');
      loadDetail();
    } catch {}
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('답변을 삭제하시겠습니까?')) return;
    try {
      await deleteComment(inquiryId, commentId);
      Toast.success('답변이 삭제되었습니다.');
      loadDetail();
    } catch {}
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;
  if (!detail) return null;

  const safeComments = detail.comments || [];
  const parentComments = safeComments.filter(c => !c.parentId);
  const childComments = safeComments.filter(c => c.parentId);

  const isDeletedComment = (comment) => comment.content === '삭제된 댓글입니다.' || comment.isDeleted;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">문의 상세 (관리자)</h2>
          <p className="text-sm text-slate-500 mt-1">가맹점의 문의 내용을 확인하고 답변을 관리합니다.</p>
        </div>
        <button 
          onClick={() => navigate('/admin/inquiries')} 
          className="px-5 py-2 bg-white border border-slate-300 text-slate-600 text-sm font-bold rounded-xl hover:bg-slate-50 transition-colors shadow-sm"
        >
          목록으로
        </button>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-8 py-6 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3 mb-2">
            <h3 className="text-xl font-bold text-slate-800">{detail.title}</h3>
            <InquiryStatusBadge isRead={detail.isRead} />
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-500">
            <span className="font-semibold text-slate-700">{detail.authorName}</span>
            <span>|</span>
            <span>{new Date(detail.createdAt).toLocaleString('ko-KR')}</span>
          </div>
        </div>

        <div className="px-8 py-10 min-h-[200px]">
          <p className="text-base text-slate-700 whitespace-pre-wrap leading-relaxed">{detail.content}</p>
        </div>

        <div className="px-8 py-8 bg-slate-50/80 border-t border-slate-100">
          <h4 className="text-sm font-bold text-slate-800 mb-5 flex items-center gap-2">
            <span>답변 및 피드백</span>
            <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full text-xs">{safeComments.length}</span>
          </h4>
          
          <div className="space-y-5 mb-8">
            {parentComments.length === 0 ? (
              <p className="text-sm text-slate-400 py-6 text-center border border-dashed border-slate-300 rounded-xl bg-slate-50">아직 등록된 답변이 없습니다.</p>
            ) : (
              parentComments.map((comment, index) => {
                const parentKey = comment.id ? `parent-${comment.id}` : `parent-idx-${index}`;
                const isMine = checkIsMine(comment);
                
                return (
                  <div key={parentKey} className="flex flex-col gap-3">
                    {/* 부모 댓글 */}
                    <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm transition-all">
                      <div className="flex justify-between items-center mb-3">
                        <div className="flex items-center gap-2 text-sm">
                          <span className={`font-bold ${isMine ? 'text-indigo-600' : 'text-slate-800'}`}>
                            {comment.authorName} {isMine && '(본사)'}
                          </span>
                          <span className="text-xs text-slate-400">{new Date(comment.createdAt || Date.now()).toLocaleString('ko-KR')}</span>
                        </div>
                        
                        {!isDeletedComment(comment) && (
                          <div className="flex items-center gap-3">
                            <button 
                              onClick={() => { setReplyTo({ id: comment.id, name: comment.authorName }); handleEditCancel(); }} 
                              className="text-xs text-slate-500 hover:text-indigo-600 font-medium"
                            >
                              답글
                            </button>
                            {isMine && (
                              <>
                                <button onClick={() => handleEditStart(comment)} className="text-xs text-indigo-500 hover:text-indigo-700 font-medium">수정</button>
                                <button onClick={() => handleDeleteComment(comment.id)} className="text-xs text-rose-500 hover:text-rose-700 font-medium">삭제</button>
                              </>
                            )}
                          </div>
                        )}
                      </div>

                      {editingCommentId === comment.id ? (
                        <div className="mt-3">
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            className="w-full px-4 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            rows="2"
                          />
                          <div className="flex justify-end gap-2 mt-2">
                            <button onClick={handleEditCancel} className="px-3 py-1.5 text-xs font-bold text-slate-500 hover:bg-slate-100 rounded-lg">취소</button>
                            <button onClick={() => handleEditSubmit(comment.id)} className="px-3 py-1.5 text-xs font-bold bg-slate-800 text-white hover:bg-slate-900 rounded-lg">저장</button>
                          </div>
                        </div>
                      ) : (
                        <p className={`text-sm whitespace-pre-wrap leading-relaxed ${isDeletedComment(comment) ? 'text-slate-400 italic' : 'text-slate-600'}`}>
                          {comment.content}
                        </p>
                      )}
                    </div>

                    {/* 자식 댓글(답댓글) */}
                    {childComments
                      .filter(reply => reply.parentId === comment.id)
                      .map((reply, childIndex) => {
                        const childKey = reply.id ? `child-${reply.id}` : `child-idx-${childIndex}`;
                        const isChildMine = checkIsMine(reply);
                        
                        return (
                          <div key={childKey} className="ml-10 relative">
                            <div className="absolute -left-5 top-5 w-4 h-4 border-l-2 border-b-2 border-slate-300 rounded-bl-lg" />
                            <div className="bg-slate-50/70 p-4 rounded-xl border border-slate-200 shadow-sm transition-all">
                              <div className="flex justify-between items-center mb-2">
                                <div className="flex items-center gap-2 text-sm">
                                  <span className={`font-bold ${isChildMine ? 'text-indigo-600' : 'text-slate-800'}`}>
                                    {reply.authorName} {isChildMine && '(본사)'}
                                  </span>
                                  <span className="text-xs text-slate-400">{new Date(reply.createdAt || Date.now()).toLocaleString('ko-KR')}</span>
                                </div>

                                {!isDeletedComment(reply) && isChildMine && (
                                  <div className="flex items-center gap-3">
                                    <button onClick={() => handleEditStart(reply)} className="text-xs text-indigo-500 hover:text-indigo-700 font-medium">수정</button>
                                    <button onClick={() => handleDeleteComment(reply.id)} className="text-xs text-rose-500 hover:text-rose-700 font-medium">삭제</button>
                                  </div>
                                )}
                              </div>
                              
                              {editingCommentId === reply.id ? (
                                <div className="mt-3">
                                  <textarea
                                    value={editContent}
                                    onChange={(e) => setEditContent(e.target.value)}
                                    className="w-full px-4 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                    rows="2"
                                  />
                                  <div className="flex justify-end gap-2 mt-2">
                                    <button onClick={handleEditCancel} className="px-3 py-1.5 text-xs font-bold text-slate-500 hover:bg-slate-100 rounded-lg">취소</button>
                                    <button onClick={() => handleEditSubmit(reply.id)} className="px-3 py-1.5 text-xs font-bold bg-slate-800 text-white hover:bg-slate-900 rounded-lg">저장</button>
                                  </div>
                                </div>
                              ) : (
                                <p className={`text-sm whitespace-pre-wrap leading-relaxed ${isDeletedComment(reply) ? 'text-slate-400 italic' : 'text-slate-600'}`}>
                                  {reply.content}
                                </p>
                              )}
                            </div>
                          </div>
                        );
                      })}
                  </div>
                );
              })
            )}
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
            {replyTo && (
              <div className="flex items-center justify-between bg-indigo-50 px-4 py-2 rounded-lg mb-3 border border-indigo-100">
                <span className="text-xs font-semibold text-indigo-700">
                  <span className="font-bold">@{replyTo.name}</span> 님에게 답글 작성 중...
                </span>
                <button onClick={() => setReplyTo(null)} className="text-xs font-bold text-indigo-400 hover:text-indigo-600">✕ 취소</button>
              </div>
            )}
            <form onSubmit={handleCommentSubmit} className="flex gap-3">
              <input 
                type="text" 
                value={commentContent} 
                onChange={(e) => setCommentContent(e.target.value)} 
                placeholder={replyTo ? "답글 내용을 입력하세요..." : "가맹점에 전달할 답변을 입력하세요..."} 
                className="flex-1 px-5 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm" 
              />
              <button type="submit" disabled={submitting || !commentContent.trim()} className="px-6 py-3 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 disabled:opacity-50 transition-colors shadow-sm whitespace-nowrap">
                {replyTo ? '답글 등록' : '답변 등록'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}