import React from 'react'
import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import { Toaster } from 'react-hot-toast'

// Layouts & Guards
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import AdminSidebar from './components/AdminSidebar'
import ProtectedRoute from './components/ProtectedRoute'

// Customer Pages
import Home from './pages/Home'
import Catalog from './pages/Catalog'
import ProductDetail from './pages/ProductDetail'
import CartPage from './pages/CartPage'
import WishlistPage from './pages/WishlistPage'
import ProfilePage from './pages/ProfilePage'
import CheckoutPage from './pages/CheckoutPage'
import OrderHistoryPage from './pages/OrderHistoryPage'
import OrderSuccessPage from './pages/OrderSuccessPage'
import Login from './pages/Login'
import Register from './pages/Register'

// Admin Pages
import AdminDashboard from './pages/AdminDashboard'
import AdminProducts from './pages/AdminProducts'
import AdminCategories from './pages/AdminCategories'
import AdminOrders from './pages/AdminOrders'
import AdminCoupons from './pages/AdminCoupons'
import AdminCustomers from './pages/AdminCustomers'

// Layout Wrapper
const AppLayout = () => {
  const location = useLocation()
  const isAdminRoute = location.pathname.startsWith('/admin')

  if (isAdminRoute) {
    return (
      <div className="flex min-h-screen bg-[#0c0f17] text-gray-100">
        <AdminSidebar />
        <main className="flex-1 p-8 overflow-y-auto">
          <Routes>
            <Route path="/admin/dashboard" element={<ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>} />
            <Route path="/admin/products" element={<ProtectedRoute adminOnly><AdminProducts /></ProtectedRoute>} />
            <Route path="/admin/categories" element={<ProtectedRoute adminOnly><AdminCategories /></ProtectedRoute>} />
            <Route path="/admin/orders" element={<ProtectedRoute adminOnly><AdminOrders /></ProtectedRoute>} />
            <Route path="/admin/coupons" element={<ProtectedRoute adminOnly><AdminCoupons /></ProtectedRoute>} />
            <Route path="/admin/customers" element={<ProtectedRoute adminOnly><AdminCustomers /></ProtectedRoute>} />
          </Routes>
        </main>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-[#0c0f17] text-gray-100">
      <Navbar />
      <main className="flex-grow max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/catalog" element={<Catalog />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Protected Customer Routes */}
          <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
          <Route path="/order-success" element={<ProtectedRoute><OrderSuccessPage /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><OrderHistoryPage /></ProtectedRoute>} />
          <Route path="/wishlist" element={<ProtectedRoute><WishlistPage /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        </Routes>
      </main>
      <Footer />
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Toaster 
            position="bottom-right" 
            toastOptions={{
              style: {
                background: '#1a1f2e',
                color: '#fff',
                border: '1px solid #2d3348'
              }
            }}
          />
          <AppLayout />
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
