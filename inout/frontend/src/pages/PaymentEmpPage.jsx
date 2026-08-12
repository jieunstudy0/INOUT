import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Toast } from '../utils/toast';

/**
 * 레거시 직원 결제 페이지 — 3단계 발주 모델에서는 점주 승인·결제만 허용.
 */
export default function PaymentEmpPage() {
  const navigate = useNavigate();

  useEffect(() => {
    Toast.info('직원 기안은 점주가 승인·결제한 뒤 본사로 전달됩니다.');
    navigate('/emp/orders', { replace: true });
  }, [navigate]);

  return null;
}
