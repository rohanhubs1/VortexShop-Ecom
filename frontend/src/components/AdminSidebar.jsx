import React from 'react'
import { NavLink, Link } from 'react-router-dom'
import { LayoutDashboard, ShoppingBag, FolderTree, ClipboardList, Tag, Users, ShieldAlert, ArrowLeft } from 'lucide-react'

const AdminSidebar = () => {
  const menuItems = [
    { name: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard },
    { name: 'Products', path: '/admin/products', icon: ShoppingBag },
    { name: 'Categories', path: '/admin/categories', icon: FolderTree },
    { name: 'Orders', path: '/admin/orders', icon: ClipboardList },
    { name: 'Coupons', path: '/admin/coupons', icon: Tag },
    { name: 'Customers', path: '/admin/customers', icon: Users },
  ]

  return (
    <aside className="w-64 bg-surface-50 border-r border-surface-200 min-h-screen flex flex-col pt-6">
      
      {/* Brand Header */}
      <div className="px-6 pb-6 border-b border-surface-200 flex items-center space-x-2">
        <ShieldAlert className="h-6 w-6 text-brand-600" />
        <span className="font-extrabold text-lg text-surface-900 tracking-wider">ADMIN CONTROL</span>
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
                `flex items-center space-x-3 px-4 py-2.5 rounded-lg text-sm font-bold transition-all ${
                  isActive
                    ? 'bg-brand-50 text-brand-700 border-l-4 border-brand-500 shadow-sm'
                    : 'text-surface-600 hover:bg-white hover:text-brand-600 hover:shadow-sm'
                }`
              }
            >
              <Icon className="h-5 w-5" />
              <span>{item.name}</span>
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
