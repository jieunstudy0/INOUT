import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getInquiryDetail, deleteInquiry, createComment, deleteComment, updateComment, downloadInquiryFile } from '../api/inquiryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import PersonName from '../components/common/PersonName';

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

export default function InquiryDetailEmpPage() {
  const { inquiryId } = useParams();
  const navigate = useNavigate();

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  const [commentContent, setCommentContent] = useState('');
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

  const handleDeleteInquiry = async () => {
    if (!window.confirm('이 문의글을 삭제하시겠습니까?')) return;
    try {
      await deleteInquiry(inquiryId);
      Toast.success('문의글이 삭제되었습니다.');
      navigate('/emp/inquiries', { replace: true });
    } catch {}
  };

  const handleFileDownload = async () => {
    const fileName = detail?.originalFileName || detail?.fileName || 'attachment';
    setDownloading(true);
    try {
      const response = await downloadInquiryFile(inquiryId);
      const blob = response.data;

      if (blob?.type?.includes('application/json')) {
        const text = await blob.text();
        try {
          const json = JSON.parse(text);
          Toast.error(json?.header?.message || json?.message || '파일 다운로드에 실패했습니다.');
        } catch {
          Toast.error('파일 다운로드에 실패했습니다.');
        }
        return;
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      Toast.success('파일 다운로드를 시작했습니다.');
    } catch {
      /* interceptor toast */
    } finally {
      setDownloading(false);
    }
  };

  const handleCommentSubmit = async (e) => {
    e.preventDefault();
    if (!commentContent.trim()) return;
    try {
      await createComment(inquiryId, {
        content: commentContent.trim(),
        parentId: replyTo ? replyTo.id : null,
      });
      setCommentContent('');
      setReplyTo(null);
      loadDetail();
    } catch {}
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
      Toast.success('댓글이 수정되었습니다.');
      setEditingCommentId(null);
      setEditContent('');
      loadDetail();
    } catch {}
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
    try {
      await deleteComment(inquiryId, commentId);
      Toast.success('댓글이 삭제되었습니다.');
      loadDetail();
    } catch {}
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;
  if (!detail) return null;

  const safeComments = detail.comments || [];
  const parentComments = safeComments.filter((c) => !c.parentId);
  const childComments = safeComments.filter((c) => c.parentId);

  const isDeletedComment = (comment) => comment.content === '삭제된 댓글입니다.' || comment.isDeleted;

  const fileName = detail.originalFileName || detail.fileName || detail.attachmentName;
  const hasAttachment = Boolean(
    fileName && (detail.savedFilePath || detail.filePath || detail.fileUrl || detail.originalFileName),
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">문의 상세</h2>
          <p className="text-sm text-slate-500 mt-1">문의하신 내용과 본사의 답변을 확인합니다.</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => navigate('/emp/inquiries')}
            className="px-4 py-2 bg-white border border-slate-300 text-slate-600 text-sm font-bold rounded-xl hover:bg-slate-50"
          >
            목록으로
          </button>
          <button
            onClick={handleDeleteInquiry}
            className="px-4 py-2 bg-rose-50 text-rose-600 text-sm font-bold rounded-xl hover:bg-rose-100"
          >
            글 삭제
          </button>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-8 py-6 border-b border-slate-100 bg-slate-50/50">
          <h3 className="text-xl font-bold text-slate-800 mb-2">{detail.title}</h3>
          <div className="flex items-center gap-3 text-sm text-slate-500">
            <span><PersonName name={detail.authorName || '작성자'} /></span>
            <span>|</span>
            <span>{new Date(detail.createdAt).toLocaleString('ko-KR')}</span>
          </div>
        </div>

        <div className="px-8 py-10 min-h-[180px]">
          <p className="text-base text-slate-700 whitespace-pre-wrap leading-relaxed">{detail.content}</p>
        </div>

        {hasAttachment && (
          <div className="px-8 py-4 bg-slate-50/90 border-t border-slate-100 flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-10 h-10 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center font-bold text-lg shrink-0">
                📎
              </div>
              <div className="min-w-0">
                <p className="text-sm font-semibold text-slate-800 truncate">{fileName}</p>
                <p className="text-xs text-slate-400">첨부파일을 다운로드할 수 있습니다.</p>
              </div>
            </div>
            <button
              type="button"
              onClick={handleFileDownload}
              disabled={downloading}
              className="px-4 py-2 bg-indigo-600 text-white text-xs font-bold rounded-xl hover:bg-indigo-700 transition-colors shadow-sm disabled:opacity-50 whitespace-nowrap"
            >
              {downloading ? '다운로드 중…' : '다운로드'}
            </button>
          </div>
        )}

        <div className="px-8 py-6 bg-slate-50/80 border-t border-slate-100">
          <h4 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
            <span>답변 및 댓글</span>
            <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full text-xs">{safeComments.length}</span>
          </h4>

          <div className="space-y-5 mb-8">
            {parentComments.length === 0 ? (
              <p className="text-sm text-slate-500 py-4 text-center">아직 등록된 답변이 없습니다.</p>
            ) : (
              parentComments.map((comment, index) => {
                const parentKey = comment.id ? `parent-${comment.id}` : `parent-idx-${index}`;
                const isMine = checkIsMine(comment);

                return (
                  <div key={parentKey} className="flex flex-col gap-3">
                    <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm transition-all">
                      <div className="flex justify-between items-center mb-2">
                        <div className="flex items-center gap-2 text-sm">
                          <span className={`font-bold ${isMine ? 'text-slate-800' : 'text-indigo-600'}`}>
                            <PersonName name={comment.authorName} />
                          </span>
                          <span className="text-xs text-slate-400">{new Date(comment.createdAt || Date.now()).toLocaleString('ko-KR')}</span>
                        </div>

                        {!isDeletedComment(comment) && (
                          <div className="flex items-center gap-3">
                            <button
                              onClick={() => { setReplyTo({ id: comment.id, name: comment.authorName }); handleEditCancel(); }}
                              className="text-xs text-slate-500 hover:text-slate-800 font-medium"
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
                            className="w-full px-4 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
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

                    {childComments
                      .filter((reply) => reply.parentId === comment.id)
                      .map((reply, childIndex) => {
                        const childKey = reply.id ? `child-${reply.id}` : `child-idx-${childIndex}`;
                        const isChildMine = checkIsMine(reply);

                        return (
                          <div key={childKey} className="ml-10 relative">
                            <div className="absolute -left-5 top-5 w-4 h-4 border-l-2 border-b-2 border-slate-300 rounded-bl-lg" />
                            <div className="bg-slate-50/70 p-4 rounded-xl border border-slate-200 shadow-sm transition-all">
                              <div className="flex justify-between items-center mb-2">
                                <div className="flex items-center gap-2 text-sm">
                                  <span className={`font-bold ${isChildMine ? 'text-slate-800' : 'text-indigo-600'}`}>
                                    <PersonName name={reply.authorName} />
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
                                    className="w-full px-4 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
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
              <div className="flex items-center justify-between bg-slate-100 px-4 py-2 rounded-lg mb-3 border border-slate-200">
                <span className="text-xs font-semibold text-slate-700">
                  <span className="font-bold">@{replyTo.name}</span> 님에게 답글 작성 중...
                </span>
                <button onClick={() => setReplyTo(null)} className="text-xs font-bold text-slate-400 hover:text-slate-600">✕ 취소</button>
              </div>
            )}

            <form onSubmit={handleCommentSubmit} className="flex gap-2">
              <input
                type="text"
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                placeholder={replyTo ? '답글 내용을 입력하세요...' : '답변이나 추가 문의사항을 남겨주세요...'}
                className="flex-1 px-5 py-3 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-500"
              />
              <button type="submit" disabled={!commentContent.trim()} className="px-6 py-3 bg-slate-800 text-white text-sm font-bold rounded-xl hover:bg-slate-900 disabled:opacity-50 transition-colors whitespace-nowrap">
                등록
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
