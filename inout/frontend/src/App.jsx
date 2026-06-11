import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import LoginPage     from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import DashboardEmpPage from './pages/DashboardEmpPage';
import OrderAdmPage  from './pages/OrderAdmPage';
import StockPage     from './pages/StockPage';
import StockEmpPage  from './pages/StockEmpPage'; 
import CartEmpPage   from './pages/CartEmpPage';
import OrderEmpPage  from './pages/OrderEmpPage';
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
import DepositAdmPage from './pages/DepositAdmPage';
import DeliveryAdmPage from './pages/DeliveryAdmPage';

function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-slate-400">
      <p className="text-5xl font-bold text-slate-200 mb-4">404</p>
      <p className="text-sm font-medium">페이지를 찾을 수 없습니다.</p>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastContainer />
      <Routes>

        <Route path="/login" element={<LoginPage />} />

        <Route element={<Layout />}>
           {/* 관리자 경로 */}
           <Route path="/admin/dashboard" element={<DashboardPage />} />
           <Route path="/admin/orders"    element={<OrderAdmPage />} />
           <Route path="/admin/stock"     element={<StockPage />} />
           <Route path="/admin/inquiries" element={<InquiryAdmPage />} />
           <Route path="/admin/users"     element={<UserAdmPage />} />
           <Route path="/admin/inquiries" element={<InquiryAdmPage />} />
           <Route path="/admin/inquiries/:inquiryId" element={<InquiryDetailAdmPage />} />
           <Route path="/admin/deposit"   element={<DepositAdmPage />} />
           <Route path="/admin/delivery"  element={<DeliveryAdmPage />} />
           
           
           {/* 직원 경로 */}
           <Route path="/emp/dashboard"   element={<DashboardEmpPage />} />
           <Route path="/emp/stocks"      element={<StockEmpPage />} />
           <Route path="/emp/stock-use"   element={<StockUseEmpPage />} />
           <Route path="/emp/cart"        element={<CartEmpPage />} />
           <Route path="/emp/orders"      element={<OrderEmpPage />} />
           <Route path="/emp/inquiries"   element={<InquiryEmpPage />} />
           <Route path="/emp/inquiries/new" element={<InquiryCreatePage />} />
           <Route path="/emp/inquiries/:inquiryId" element={<InquiryDetailEmpPage />} />
           <Route path="/emp/profile"     element={<ProfileEmpPage />} />  
           <Route path="/emp/deposit"     element={<DepositEmpPage />} />  
           <Route path="/emp/payment/:orderId" element={<PaymentEmpPage />} />   
        </Route>

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}