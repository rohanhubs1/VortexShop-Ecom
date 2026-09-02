import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { ShoppingCart, ShoppingBag, Heart, User, LogOut, Search, LayoutDashboard, Menu, X, MessageSquare } from 'lucide-react'
const Navbar = () => {
  const { isAuthenticated, isAdmin, user, logout } = useAuth()
  const { cartCount } = useCart()
  const [search, setSearch] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const navigate = useNavigate()

  const handleSearch = (e) => {
    e.preventDefault()
    if (search.trim()) {
      navigate(`/catalog?search=${encodeURIComponent(search.trim())}`)
    }
  }

  return (
    <nav className="sticky top-0 z-50 bg-white shadow-sm border-b border-surface-200 transition-all duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <span className="text-2xl font-black tracking-tight text-brand-600">
              VORTEX
            </span>
          </Link>

          {/* Search Bar */}
          <form onSubmit={handleSearch} className="hidden md:flex flex-1 max-w-lg mx-8 relative">
            <input
              type="text"
              placeholder="Search for products, brands, and more..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-surface-100 border border-surface-200 rounded-lg py-2 pl-4 pr-10 text-sm text-surface-900 focus:outline-none focus:bg-white focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition-colors"
            />
            <button type="submit" className="absolute right-3 top-2.5 text-surface-500 hover:text-brand-600">
              <Search className="h-4 w-4" />
            </button>
          </form>

          {/* Nav Items */}
          <div className="hidden md:flex items-center space-x-6">
            <Link to="/catalog" className="text-sm font-medium text-surface-600 hover:text-brand-600 transition-colors">Catalog</Link>
            
            {isAuthenticated ? (
              <>
                <Link to="/wishlist" className="relative p-1 text-surface-600 hover:text-brand-600 transition-colors">
                  <Heart className="h-5 w-5" />
                </Link>

                <Link to="/cart" className="relative p-1 text-surface-600 hover:text-brand-600 transition-colors">
                  <ShoppingCart className="h-5 w-5" />
                  {cartCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-red-500 text-white font-bold text-[10px] h-4 w-4 rounded-full flex items-center justify-center shadow-sm">
                      {cartCount}
                    </span>
                  )}
                </Link>

                {isAdmin && (
                  <Link to="/admin/dashboard" className="flex items-center space-x-1 text-sm font-medium text-brand-600 hover:text-brand-700 transition-colors">
                    <LayoutDashboard className="h-4 w-4" />
                    <span>Admin</span>
                  </Link>
                )}

                {/* User Dropdown */}
                <div className="relative group">
                  <button className="flex items-center space-x-2 text-sm text-surface-600 hover:text-brand-600 transition-colors py-1.5 focus:outline-none">
                    <div className="h-8 w-8 rounded-full bg-surface-100 flex items-center justify-center text-brand-600 font-semibold text-xs border border-surface-200">
                      {user.firstName[0]}
                    </div>
                    <span className="max-w-[80px] truncate font-medium">{user.firstName}</span>
                  </button>
                  
                  <div className="absolute right-0 top-full pt-1 w-48 opacity-0 pointer-events-none group-hover:opacity-100 group-hover:pointer-events-auto transition-all duration-200 z-50">
                    <div className="bg-white border border-surface-200 rounded-lg shadow-lg py-1">
                      <Link to="/profile" className="flex items-center px-4 py-2 text-sm text-surface-700 hover:bg-surface-50 hover:text-brand-600">
                        <User className="mr-3 h-4 w-4" /> Profile & Addresses
                      </Link>
                      <Link to="/orders" className="flex items-center px-4 py-2 text-sm text-surface-700 hover:bg-surface-50 hover:text-brand-600">
                        <ShoppingBag className="mr-3 h-4 w-4" /> Order History
                      </Link>
                      <Link to="/support" className="flex items-center px-4 py-2 text-sm text-surface-700 hover:bg-surface-50 hover:text-brand-600">
                        <MessageSquare className="mr-3 h-4 w-4" /> Support Tickets
                      </Link>
                      <div className="border-t border-surface-100 my-1"></div>
                      <button 
                        onClick={() => { logout(); navigate('/login'); }}
                        className="flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50 w-full text-left font-medium"
                      >
                        <LogOut className="mr-3 h-4 w-4" /> Logout
                      </button>
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-4">
                <Link to="/login" className="text-sm font-medium text-surface-600 hover:text-brand-600 transition-colors">Sign In</Link>
                <Link to="/register" className="btn-primary">Sign Up</Link>
              </div>
            )}
          </div>

          {/* Mobile Menu Toggle */}
          <div className="md:hidden flex items-center space-x-4">
            {isAuthenticated && (
              <Link to="/cart" className="relative p-1 text-surface-600 hover:text-brand-600">
                <ShoppingCart className="h-5 w-5" />
                {cartCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold h-4 w-4 rounded-full flex items-center justify-center">
                    {cartCount}
                  </span>
                )}
              </Link>
            )}
            <button onClick={() => setIsOpen(!isOpen)} className="text-surface-600 hover:text-brand-600 focus:outline-none">
              {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>

        </div>
      </div>

      {/* Mobile Menu */}
      {isOpen && (
        <div className="md:hidden bg-white border-t border-surface-200 px-4 py-4 space-y-3 shadow-inner">
          <form onSubmit={handleSearch} className="relative w-full">
            <input
              type="text"
              placeholder="Search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-surface-100 border border-surface-200 rounded-lg py-2 pl-4 pr-10 text-sm text-surface-900"
            />
            <button type="submit" className="absolute right-3 top-2.5 text-surface-500">
              <Search className="h-4 w-4" />
            </button>
          </form>

          <Link to="/catalog" onClick={() => setIsOpen(false)} className="block py-2 text-sm font-medium text-surface-700">Catalog</Link>
          
          {isAuthenticated ? (
            <>
              <Link to="/wishlist" onClick={() => setIsOpen(false)} className="block py-2 text-sm font-medium text-surface-700">Wishlist</Link>
              <Link to="/profile" onClick={() => setIsOpen(false)} className="block py-2 text-sm font-medium text-surface-700">My Profile</Link>
              <Link to="/orders" onClick={() => setIsOpen(false)} className="block py-2 text-sm font-medium text-surface-700">Order History</Link>
              <Link to="/support" onClick={() => setIsOpen(false)} className="block py-2 text-sm font-medium text-surface-700">Support Tickets</Link>
              {isAdmin && (
                <Link to="/admin/dashboard" onClick={() => setIsOpen(false)} className="block py-2 text-sm text-brand-600 font-semibold">Admin Panel</Link>
              )}
              <button onClick={() => { logout(); setIsOpen(false); }} className="w-full py-2 text-sm text-red-600 font-medium text-left">Logout</button>
            </>
          ) : (
            <div className="pt-2 flex flex-col space-y-2">
              <Link to="/login" onClick={() => setIsOpen(false)} className="py-2 text-center text-sm font-medium text-surface-600">Sign In</Link>
              <Link to="/register" onClick={() => setIsOpen(false)} className="btn-primary text-center">Sign Up</Link>
            </div>
          )}
        </div>
      )}
    </nav>
  )
}

export default Navbar
