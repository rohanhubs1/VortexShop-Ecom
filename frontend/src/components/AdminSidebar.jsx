import React, { useState, useEffect, useRef } from 'react'
import { NavLink, Link } from 'react-router-dom'
import { LayoutDashboard, ShoppingBag, FolderTree, ClipboardList, Tag, Users, ShieldAlert, ArrowLeft, Headphones, Bell } from 'lucide-react'
import API from '../api/axios'

const AdminSidebar = () => {
  const [notifications, setNotifications] = useState({ pendingOrders: 0, openTickets: 0 })
  const [showDropdown, setShowDropdown] = useState(false)
  const dropdownRef = useRef(null)

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const res = await API.get('/admin/notifications/summary')
        setNotifications(res.data)
      } catch (err) {
        console.error('Failed to load notifications')
      }
    }
    fetchNotifications()
    // Poll every 30 seconds
    const interval = setInterval(fetchNotifications, 30000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

  const menuItems = [
    { name: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard },
    { name: 'Products', path: '/admin/products', icon: ShoppingBag },
    { name: 'Categories', path: '/admin/categories', icon: FolderTree },
    { name: 'Orders', path: '/admin/orders', icon: ClipboardList, badge: notifications.pendingOrders },
    { name: 'Coupons', path: '/admin/coupons', icon: Tag },
    { name: 'Customers', path: '/admin/customers', icon: Users },
    { name: 'Support', path: '/admin/support', icon: Headphones, badge: notifications.openTickets },
  ]

  const totalNotifications = notifications.pendingOrders + notifications.openTickets

  return (
    <aside className="w-64 bg-surface-50 border-r border-surface-200 min-h-screen flex flex-col pt-6">
      
      {/* Brand Header */}
      <div className="px-6 pb-6 border-b border-surface-200 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <ShieldAlert className="h-6 w-6 text-brand-600" />
          <span className="font-extrabold text-lg text-surface-900 tracking-wider">ADMIN</span>
        </div>
        <div className="relative" ref={dropdownRef}>
          <button 
            onClick={() => setShowDropdown(!showDropdown)}
            className="relative cursor-pointer p-1.5 rounded-lg hover:bg-surface-100 transition-colors focus:outline-none"
            aria-label="Notifications"
          >
            <Bell className={`h-5 w-5 ${showDropdown ? 'text-brand-600' : 'text-surface-400'} hover:text-brand-600 transition-colors`} />
            {totalNotifications > 0 && (
              <span className="absolute top-0.5 right-0.5 bg-red-500 text-white text-[9px] font-black w-4 h-4 rounded-full flex items-center justify-center animate-pulse shadow-sm">
                {totalNotifications > 99 ? '99+' : totalNotifications}
              </span>
            )}
          </button>

          {showDropdown && (
            <div className="absolute left-[-200px] mt-2 w-72 bg-white border border-surface-200 rounded-xl shadow-xl z-50 py-2 divide-y divide-surface-100 text-surface-800">
              <div className="px-4 py-2 flex items-center justify-between">
                <span className="font-bold text-sm text-surface-900">Notifications</span>
                {totalNotifications > 0 && (
                  <span className="text-[10px] bg-brand-50 text-brand-600 font-extrabold px-2 py-0.5 rounded-full">
                    {totalNotifications} New
                  </span>
                )}
              </div>
              <div className="py-1 max-h-60 overflow-y-auto">
                {totalNotifications === 0 ? (
                  <div className="px-4 py-6 text-center text-surface-400">
                    <Bell className="h-8 w-8 mx-auto mb-2 text-surface-300" />
                    <p className="text-xs font-semibold">All caught up!</p>
                    <p className="text-[10px] mt-0.5">No pending orders or tickets.</p>
                  </div>
                ) : (
                  <>
                    {notifications.pendingOrders > 0 && (
                      <Link 
                        to="/admin/orders" 
                        onClick={() => setShowDropdown(false)}
                        className="flex items-start gap-3 px-4 py-3 hover:bg-surface-50 transition-colors"
                      >
                        <div className="p-2 bg-amber-50 rounded-lg text-amber-600 shrink-0 mt-0.5">
                          <ClipboardList className="h-4 w-4" />
                        </div>
                        <div className="space-y-0.5 text-left">
                          <p className="text-xs font-bold text-surface-900">Pending Orders</p>
                          <p className="text-[10px] text-surface-500 font-medium">
                            You have <span className="font-bold text-amber-600">{notifications.pendingOrders}</span> order(s) awaiting processing.
                          </p>
                        </div>
                      </Link>
                    )}
                    {notifications.openTickets > 0 && (
                      <Link 
                        to="/admin/support" 
                        onClick={() => setShowDropdown(false)}
                        className="flex items-start gap-3 px-4 py-3 hover:bg-surface-50 transition-colors"
                      >
                        <div className="p-2 bg-blue-50 rounded-lg text-blue-600 shrink-0 mt-0.5">
                          <Headphones className="h-4 w-4" />
                        </div>
                        <div className="space-y-0.5 text-left">
                          <p className="text-xs font-bold text-surface-900">Support Tickets</p>
                          <p className="text-[10px] text-surface-500 font-medium">
                            There are <span className="font-bold text-blue-600">{notifications.openTickets}</span> open customer inquiries.
                          </p>
                        </div>
                      </Link>
                    )}
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 py-6 space-y-2">
        {menuItems.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.name}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center justify-between px-4 py-2.5 rounded-lg text-sm font-bold transition-all ${
                  isActive
                    ? 'bg-brand-50 text-brand-700 border-l-4 border-brand-500 shadow-sm'
                    : 'text-surface-600 hover:bg-white hover:text-brand-600 hover:shadow-sm'
                }`
              }
            >
              <div className="flex items-center space-x-3">
                <Icon className="h-5 w-5" />
                <span>{item.name}</span>
              </div>
              {item.badge > 0 && (
                <span className="bg-brand-500 text-white text-[10px] font-black px-2 py-0.5 rounded-full">
                  {item.badge}
                </span>
              )}
            </NavLink>
          )
        })}
      </nav>

      {/* Return to store */}
      <div className="p-4 border-t border-surface-200">
        <Link to="/" className="flex items-center justify-center space-x-2 w-full bg-white border border-surface-200 hover:border-surface-300 hover:bg-surface-50 text-surface-700 py-2.5 rounded-lg text-xs font-bold transition-colors shadow-sm">
          <ArrowLeft className="h-4 w-4" />
          <span>Return to Store</span>
        </Link>
      </div>

    </aside>
  )
}

export default AdminSidebar
