import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import OwnerLayout from './components/layout/OwnerLayout';
import RoleGuard from './components/auth/RoleGuard';
import LoginPage from './pages/LoginPage';
import FindAccountPage from './pages/FindAccountPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import DashboardPage from './pages/admin/AdminDashboard';
import DashboardEmpPage from './pages/emp/EmpDashboard';
import OrderAdmPage from './pages/OrderAdmPage';
import StockPage from './pages/StockPage';
import StockEmpPage from './pages/StockEmpPage';
import CartEmpPage from './pages/CartEmpPage';
import OrderEmpPage from './pages/OrderEmpPage';
import ToastContainer from './components/common/ToastContainer';
import InquiryAdmPage from './pages/InquiryAdmPage';
import InquiryEmpPage from './pages/InquiryEmpPage';
import InquiryCreatePage from './pages/InquiryCreatePage';
import ProfileEmpPage from './pages/ProfileEmpPage';
import StockUseEmpPage from './pages/StockUseEmpPage';
import DepositEmpPage from './pages/DepositEmpPage';
import PaymentEmpPage from './pages/PaymentEmpPage';
import InquiryDetailEmpPage from './pages/InquiryDetailEmpPage';
import UserAdmPage from './pages/UserAdmPage';
import InquiryDetailAdmPage from './pages/InquiryDetailAdmPage';
import InquiryOwnerPage from './pages/InquiryOwnerPage';
import InquiryDetailOwnerPage from './pages/InquiryDetailOwnerPage';
import DepositAdmPage from './pages/DepositAdmPage';
import DeliveryAdmPage from './pages/DeliveryAdmPage';
import VacationEmpPage from './pages/VacationEmpPage';
import VacationRegisterEmpPage from './pages/VacationRegisterEmpPage';
import VacationEmpDetailPage from './pages/VacationEmpDetailPage';
import VacationAdmPage from './pages/VacationAdmPage';
import OwnerDashboard from './pages/owner/OwnerDashboard';
import OwnerUserManagement from './pages/OwnerUserManagement';
import OwnerDepositManagement from './pages/OwnerDepositManagement';
import OwnerOrderList from './pages/OwnerOrderList';
import OwnerLeaveApproval from './pages/OwnerLeaveApproval';
import OrderAdmDetailPage from './pages/OrderAdmDetailPage';
import { resolveHomeFromToken } from './utils/roleUtils';
import SocialOnboardingPage from './pages/SocialOnboardingPage';

function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-slate-400">
      <p className="text-5xl font-bold text-slate-200 mb-4">404</p>
      <p className="text-sm font-medium">페이지를 찾을 수 없습니다.</p>
    </div>
  );
}

function HomeRedirect() {
  const token = localStorage.getItem('accessToken');
  if (!token) return <Navigate to="/login" replace />;
  return <Navigate to={resolveHomeFromToken(token)} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastContainer />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
        <Route path="/onboarding/complete-profile" element={<SocialOnboardingPage />} />
        <Route path="/find-account" element={<FindAccountPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        {/* 관리자 / 직원 — 기존 Layout */}
        <Route
          element={(
            <RoleGuard>
              <Layout />
            </RoleGuard>
          )}
        >
          <Route path="/admin/dashboard" element={<DashboardPage />} />
          <Route path="/admin/orders" element={<OrderAdmPage />} />
          <Route path="/admin/orders/:orderId" element={<OrderAdmDetailPage />} />
          <Route path="/admin/stock" element={<StockPage />} />
          <Route path="/admin/inquiries" element={<InquiryAdmPage />} />
          <Route path="/admin/users" element={<UserAdmPage />} />
          <Route path="/admin/inquiries/:inquiryId" element={<InquiryDetailAdmPage />} />
          <Route path="/admin/deposit" element={<DepositAdmPage />} />
          <Route path="/admin/delivery" element={<DeliveryAdmPage />} />
          <Route path="/admin/leaves" element={<VacationAdmPage />} />

          <Route path="/emp/dashboard" element={<DashboardEmpPage />} />
          <Route path="/emp/stocks" element={<StockEmpPage />} />
          <Route path="/emp/stock-use" element={<StockUseEmpPage />} />
          <Route path="/emp/cart" element={<CartEmpPage />} />
          <Route path="/emp/orders" element={<OrderEmpPage />} />
          <Route path="/emp/inquiries" element={<InquiryEmpPage />} />
          <Route path="/emp/inquiries/new" element={<InquiryCreatePage />} />
          <Route path="/emp/inquiries/:inquiryId" element={<InquiryDetailEmpPage />} />
          <Route path="/emp/leaves" element={<VacationEmpPage />} />
          <Route path="/emp/leaves/register" element={<VacationRegisterEmpPage />} />
          <Route path="/emp/leaves/:id" element={<VacationEmpDetailPage />} />
          <Route path="/emp/profile" element={<ProfileEmpPage />} />
          <Route path="/emp/deposit" element={<DepositEmpPage />} />
          <Route path="/emp/payment/:orderId" element={<PaymentEmpPage />} />
        </Route>

        {/* 가맹점주 — OwnerLayout + ROLE_OWNER 가드 */}
        <Route
          path="/owner"
          element={(
            <RoleGuard allow="OWNER">
              <OwnerLayout />
            </RoleGuard>
          )}
        >
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<OwnerDashboard />} />
          <Route path="users" element={<OwnerUserManagement />} />
          <Route path="deposit" element={<OwnerDepositManagement />} />
          <Route path="orders" element={<OwnerOrderList />} />
          <Route path="leaves" element={<OwnerLeaveApproval />} />
          <Route path="inquiries" element={<InquiryOwnerPage />} />
          <Route path="inquiries/new" element={<InquiryCreatePage />} />
          <Route path="inquiries/:inquiryId" element={<InquiryDetailOwnerPage />} />
          {/* 하위 호환 별칭 */}
          <Route path="vacation" element={<Navigate to="/owner/leaves" replace />} />
          <Route path="delivery" element={<Navigate to="/owner/orders" replace />} />
        </Route>

        <Route path="/" element={<HomeRedirect />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
