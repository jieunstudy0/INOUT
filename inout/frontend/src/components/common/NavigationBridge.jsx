import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { setNavigator } from '../../utils/navigation';

/** Router 안에서 navigate 함수를 utils/navigation에 등록한다 (화면에는 아무것도 그리지 않음) */
export default function NavigationBridge() {
  const navigate = useNavigate();

  useEffect(() => {
    setNavigator(navigate);
    return () => setNavigator(null);
  }, [navigate]);

  return null;
}
