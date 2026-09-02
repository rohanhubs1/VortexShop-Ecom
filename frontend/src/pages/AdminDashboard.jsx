import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { DollarSign, ShoppingCart, Users, Package, AlertCircle } from 'lucide-react'
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, PieChart, Pie, Cell } from 'recharts'

const AdminDashboard = () => {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let intervalId;
    const fetchStats = async () => {
      try {
        const res = await API.get('/admin/dashboard')
        setStats(res.data)
      } catch (err) {
        console.error('Failed to load dashboard statistics', err)
      } finally {
        setLoading(false)
      }
    }
    
    // Initial fetch
    fetchStats()
    
    // Auto-refresh every 30 seconds
    intervalId = setInterval(() => {
      fetchStats()
    }, 30000)

    return () => clearInterval(intervalId)
  }, [])

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  // Formatting status counts for Recharts Pie Chart
  const pieData = Object.entries(stats?.orderStatusCounts || {}).map(([key, value]) => ({
    name: key,
    value: value
  })).filter(item => item.value > 0)

  const COLORS = ['#f59e0b', '#3b82f6', '#8b5cf6', '#6366f1', '#10b981', '#ef4444']

  // Use real daily sales from backend
  const chartData = stats?.dailySales?.map(item => ({
    name: item.date,
    Sales: item.sales
  })) || []

  const statCards = [
    { name: 'Total Revenue', value: `$${stats?.totalSales?.toFixed(2)}`, icon: DollarSign, color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
    { name: 'Total Orders', value: stats?.totalOrders, icon: ShoppingCart, color: 'text-brand-600 bg-brand-50 border-brand-100' },
    { name: 'Registered Customers', value: stats?.totalCustomers, icon: Users, color: 'text-purple-600 bg-purple-50 border-purple-100' },
    { name: 'Products in Catalog', value: stats?.totalProducts, icon: Package, color: 'text-indigo-600 bg-indigo-50 border-indigo-100' },
  ]

  return (
    <div className="py-6 space-y-8">
      <div className="border-b border-surface-200 pb-6">
        <h1 className="text-3xl font-extrabold text-surface-200 tracking-tight">Admin Dashboard</h1>
        <p className="text-surface-500 text-sm mt-1">Overview of business metrics, transaction stats, and inventory volumes</p>
      </div>

      {/* Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((card) => {
          const Icon = card.icon
          return (
            <div key={card.name} className="card p-6 rounded-2xl flex items-center justify-between bg-white shadow-sm hover:shadow-md transition-shadow">
              <div className="space-y-1">
                <span className="text-xs font-bold text-surface-500 uppercase tracking-wider">{card.name}</span>
                <span className="text-2xl font-black text-surface-900 block">{card.value}</span>
              </div>
              <div className={`p-4 rounded-xl shrink-0 border ${card.color}`}>
                <Icon className="h-6 w-6" />
              </div>
            </div>
          )
        })}
      </div>

      {/* Graphs Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Sales Growth Line graph */}
        <div className="lg:col-span-2 card p-6 rounded-2xl bg-white shadow-sm space-y-4">
          <h3 className="font-bold text-lg text-surface-900">Revenue Growth Rate</h3>
          <div className="h-72 w-full pt-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="salesGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0d9488" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#0d9488" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="name" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                <Tooltip contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', color: '#0f172a', borderRadius: '8px', fontSize: '12px', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                <Area type="monotone" dataKey="Sales" stroke="#0d9488" strokeWidth={2} fillOpacity={1} fill="url(#salesGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Order Status Breakdown Pie Chart */}
        <div className="card p-6 rounded-2xl bg-white shadow-sm flex flex-col justify-between h-full space-y-4">
          <h3 className="font-bold text-lg text-surface-900">Order Status Distribution</h3>
          {pieData.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center text-surface-500 py-8 bg-surface-50 rounded-xl border border-dashed border-surface-200">
              <AlertCircle className="h-8 w-8 text-surface-400 mb-2" />
              <p className="text-sm font-medium">No orders exist to compile status breakdown metrics.</p>
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center">
              <div className="h-44 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={45}
                      outerRadius={70}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {pieData.map((entry, index) => (
                         <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', color: '#0f172a', borderRadius: '8px', fontSize: '11px', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              {/* Legend */}
              <div className="grid grid-cols-2 gap-3 text-xs w-full pt-6 mt-4 border-t border-surface-100">
                {pieData.map((item, idx) => (
                  <div key={item.name} className="flex items-center space-x-2">
                    <div className="h-3 w-3 rounded-full shrink-0" style={{ backgroundColor: COLORS[idx % COLORS.length] }}></div>
                    <span className="font-bold text-surface-600 uppercase tracking-wider">{item.name}: <span className="text-surface-900">{item.value}</span></span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Top Selling Products */}
      <div className="card p-6 rounded-2xl bg-white shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-surface-100 pb-4 mb-4">
          <h3 className="font-bold text-lg text-surface-900">Top Selling Products</h3>
        </div>
        
        {stats?.topSellingProducts?.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            {stats.topSellingProducts.map((prod, index) => (
              <div key={prod.id} className="bg-surface-50 border border-surface-200 rounded-xl p-5 flex flex-col justify-between hover:border-brand-300 transition-colors">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="bg-brand-100 text-brand-700 h-6 w-6 rounded-full flex items-center justify-center text-xs font-black">#{index + 1}</span>
                  </div>
                  <h4 className="font-bold text-sm text-surface-900 line-clamp-2 leading-tight">{prod.name}</h4>
                  <p className="text-brand-600 font-extrabold text-sm mt-2">${prod.price}</p>
                </div>
                <div className="mt-4 pt-3 border-t border-surface-200 text-xs font-medium">
                  <span className={`${prod.stockQuantity > 5 ? 'text-surface-500' : 'text-red-500 font-bold'}`}>
                    Stock: {prod.stockQuantity} remaining
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-surface-50 border border-dashed border-surface-200 rounded-xl p-8 text-center">
            <p className="text-surface-500 text-sm font-medium">No top selling products data available.</p>
          </div>
        )}
      </div>

    </div>
  )
}

export default AdminDashboard
