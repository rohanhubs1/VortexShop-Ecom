import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import { useLocation, Link } from 'react-router-dom'
import API from '../api/axios'
import { Calendar, ChevronDown, ChevronUp, AlertCircle, ShoppingBag, CheckCircle, Info, Printer } from 'lucide-react'

const OrderHistoryPage = () => {
  const location = useLocation()

  // State
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [expandedOrderId, setExpandedOrderId] = useState(null)
  const [successMsg, setSuccessMsg] = useState(location.state?.orderSuccess || false)

  const loadOrders = async () => {
    try {
      const res = await API.get('/orders')
      setOrders(res.data.content)
    } catch (err) {
      console.error('Failed to load order history', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOrders()
    if (successMsg) {
      const timer = setTimeout(() => setSuccessMsg(false), 5000)
      return () => clearTimeout(timer)
    }
  }, [successMsg])

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm('Are you sure you want to cancel this order?')) return
    try {
      await API.post(`/orders/${orderId}/cancel`)
      loadOrders() // Re-fetch
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel order')
    }
  }

  const toggleExpand = (orderId) => {
    setExpandedOrderId(expandedOrderId === orderId ? null : orderId)
  }

  const getStatusColor = (status) => {
    switch (status.toUpperCase()) {
      case 'PENDING': return 'bg-amber-100 text-amber-700 border border-amber-200'
      case 'CONFIRMED': return 'bg-blue-100 text-blue-700 border border-blue-200'
      case 'PACKED': return 'bg-purple-100 text-purple-700 border border-purple-200'
      case 'SHIPPED': return 'bg-indigo-100 text-indigo-700 border border-indigo-200'
      case 'DELIVERED': return 'bg-emerald-100 text-emerald-700 border border-emerald-200'
      case 'CANCELLED': return 'bg-red-100 text-red-700 border border-red-200'
      default: return 'bg-surface-100 text-surface-600 border border-surface-200'
    }
  }

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  return (
    <div className="py-8 space-y-8 max-w-5xl mx-auto">
      
      {/* Page Title */}
      <div className="border-b border-surface-200 pb-6">
        <h1 className="text-3xl font-extrabold text-surface-300 tracking-tight">Order History</h1>
        <p className="text-surface-500 text-sm mt-1">Track shipping details and review past purchase transactions</p>
      </div>

      {successMsg && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-5 py-4 rounded-xl flex items-center space-x-3 text-sm max-w-4xl mx-auto shadow-sm animate-bounce font-medium">
          <CheckCircle className="h-5 w-5 shrink-0" />
          <span>Congratulations! Your order was placed successfully. A mock HTML notification has been triggered to Mailpit SMTP.</span>
        </div>
      )}

      {orders.length === 0 ? (
        <div className="card rounded-2xl p-16 text-center space-y-4 max-w-lg mx-auto border-dashed border-2 border-surface-200 bg-surface-50">
          <span className="text-6xl select-none">📦</span>
          <h2 className="text-2xl font-bold text-surface-900">No Orders Placed Yet</h2>
          <p className="text-surface-500 text-sm">You haven't made any purchases with VortexShop yet.</p>
          <Link to="/catalog" className="btn-primary inline-block px-8">
            Start Shopping
          </Link>
        </div>
      ) : (
        <div className="max-w-4xl mx-auto space-y-5">
          {orders.map((order) => {
            const isExpanded = expandedOrderId === order.id
            const canCancel = ['PENDING', 'CONFIRMED', 'PACKED'].includes(order.status.toUpperCase())
            
            return (
              <div key={order.id} className={`card rounded-2xl overflow-hidden border transition-all duration-200 ${isExpanded ? 'border-brand-300 shadow-md' : 'border-surface-200 shadow-sm'}`}>
                
                {/* Summary Row */}
                <div
                  onClick={() => toggleExpand(order.id)}
                  className={`p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 cursor-pointer transition-colors ${isExpanded ? 'bg-brand-50/30' : 'bg-white hover:bg-surface-50'}`}
                >
                  <div className="flex items-center space-x-4">
                    <div className="h-12 w-12 bg-brand-50 text-brand-600 rounded-xl flex items-center justify-center shrink-0 border border-brand-100">
                      <ShoppingBag className="h-6 w-6" />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-sm text-surface-900 block">{order.orderNumber}</span>
                        <button 
                          onClick={(e) => { e.stopPropagation(); window.print(); }}
                          className="print:hidden text-brand-600 hover:bg-brand-50 p-2 rounded-lg flex items-center gap-1 text-xs font-semibold"
                        >
                          <Printer className="h-4 w-4" /> Print
                        </button>
                      </div>
                      <div className="flex items-center space-x-1.5 text-xs text-surface-500 mt-1 font-medium">
                        <Calendar className="h-3.5 w-3.5" />
                        <span>{new Date(order.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between md:justify-end gap-6">
                    <div className="text-right">
                      <span className="text-xs text-surface-500 font-bold uppercase tracking-wider block">Total Amount</span>
                      <span className="font-black text-base text-surface-900">${order.finalAmount.toFixed(2)}</span>
                    </div>

                    <span className={`text-[10px] font-extrabold uppercase px-3 py-1 rounded text-center min-w-[90px] ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>

                    <div className="text-surface-400 bg-surface-100 p-1 rounded-full">
                      {isExpanded ? <ChevronUp className="h-5 w-5" /> : <ChevronDown className="h-5 w-5" />}
                    </div>
                  </div>

                </div>

                {/* Expanded Section Details */}
                {isExpanded && (
                  <div className="px-6 pb-6 pt-4 border-t border-surface-200 bg-surface-50 space-y-6">
                    
                    {/* Items table */}
                    <div className="space-y-3">
                      <span className="text-xs font-bold text-surface-500 uppercase tracking-wider block">Items Purchased</span>
                      <div className="space-y-2">
                        {order.items.map((item) => (
                          <div key={item.id} className="flex justify-between items-center text-sm bg-white p-4 rounded-xl border border-surface-200 shadow-sm">
                            <div>
                              <span className="font-bold text-surface-900 block">{item.productName}</span>
                              <span className="text-surface-500 text-xs font-medium mt-0.5 block">Qty: {item.quantity} @ ${item.price.toFixed(2)} each</span>
                            </div>
                            <span className="font-bold text-surface-900">${(item.quantity * item.price).toFixed(2)}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start text-sm text-surface-700">
                      
                      {/* Shipping address info */}
                      <div className="space-y-2 bg-white p-5 rounded-xl border border-surface-200 shadow-sm h-full">
                        <span className="text-xs font-bold text-surface-500 uppercase tracking-wider block mb-2 border-b border-surface-100 pb-2">Shipping Details</span>
                        <p className="font-bold text-surface-900">{order.shippingAddress.title}</p>
                        <p className="text-surface-600">{order.shippingAddress.streetAddress}</p>
                        <p className="text-surface-600">{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.zipCode}</p>
                        <p className="text-surface-600">{order.shippingAddress.country}</p>
                      </div>

                      {/* Financial info */}
                      <div className="space-y-3 bg-white p-5 rounded-xl border border-surface-200 shadow-sm h-full flex flex-col justify-between">
                        <div>
                          <span className="text-xs font-bold text-surface-500 uppercase tracking-wider block mb-2 border-b border-surface-100 pb-2">Financial Receipt</span>
                          <div className="flex justify-between text-surface-600 mb-1.5">
                            <span>Subtotal:</span>
                            <span className="font-medium text-surface-900">${order.totalAmount.toFixed(2)}</span>
                          </div>
                          {order.discountAmount > 0 && (
                            <div className="flex justify-between text-emerald-600 font-medium mb-1.5">
                              <span>Promo Discount:</span>
                              <span>-${order.discountAmount.toFixed(2)}</span>
                            </div>
                          )}
                        </div>
                        <div className="flex justify-between border-t border-surface-200 pt-3 font-extrabold text-surface-900 text-base">
                          <span>Paid Price:</span>
                          <span>${order.finalAmount.toFixed(2)}</span>
                        </div>
                      </div>

                    </div>

                    {/* Actions bar */}
                    {canCancel && (
                      <div className="flex flex-col sm:flex-row sm:justify-between items-start sm:items-center bg-red-50 border border-red-200 p-4 rounded-xl gap-3">
                        <div className="flex items-center space-x-2 text-red-700 text-sm font-medium">
                          <AlertCircle className="h-5 w-5 shrink-0" />
                          <span>This order is not yet shipped and can be cancelled.</span>
                        </div>
                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          className="bg-white border border-red-300 text-red-600 hover:bg-red-50 font-bold text-xs px-4 py-2 rounded-lg transition-colors w-full sm:w-auto shadow-sm"
                        >
                          Cancel Order
                        </button>
                      </div>
                    )}

                    {!canCancel && order.status.toUpperCase() !== 'CANCELLED' && (
                      <div className="flex items-center space-x-2 bg-blue-50 border border-blue-200 p-4 rounded-xl text-blue-700 text-sm font-medium">
                        <Info className="h-5 w-5 shrink-0" />
                        <span>Order is shipped/delivered and cannot be cancelled or altered.</span>
                      </div>
                    )}

                  </div>
                )}

              </div>
            )
          })}
        </div>
      )}

    </div>
  )
}

export default OrderHistoryPage
