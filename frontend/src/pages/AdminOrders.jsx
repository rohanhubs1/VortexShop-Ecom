import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { ClipboardList, Calendar, MapPin, X, ChevronLeft, ChevronRight, Eye } from 'lucide-react'

const AdminOrders = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)

  // Details Modal
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [showModal, setShowModal] = useState(false)

  const loadOrders = async () => {
    setLoading(true)
    try {
      const res = await API.get(`/admin/orders?page=${page}&size=10`)
      setOrders(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch (err) {
      console.error('Failed to load admin orders list', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOrders()
  }, [page])

  const handleStatusChange = async (orderId, newStatus) => {
    try {
      await API.put(`/admin/orders/${orderId}/status`, { status: newStatus })
      loadOrders()
      if (selectedOrder && selectedOrder.id === orderId) {
        // Update selected order modal detail state
        setSelectedOrder(prev => ({ ...prev, status: newStatus }))
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update order status')
    }
  }

  const handleOpenDetails = (order) => {
    setSelectedOrder(order)
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setSelectedOrder(null)
    setShowModal(false)
  }

  const getStatusColor = (status) => {
    switch (status.toUpperCase()) {
      case 'PENDING': return 'bg-amber-100 text-amber-800 border border-amber-200'
      case 'CONFIRMED': return 'bg-blue-100 text-blue-800 border border-blue-200'
      case 'PACKED': return 'bg-purple-100 text-purple-800 border border-purple-200'
      case 'SHIPPED': return 'bg-indigo-100 text-indigo-800 border border-indigo-200'
      case 'DELIVERED': return 'bg-emerald-100 text-emerald-800 border border-emerald-200'
      case 'CANCELLED': return 'bg-red-100 text-red-800 border border-red-200'
      default: return 'bg-surface-100 text-surface-600 border border-surface-200'
    }
  }

  return (
    <div className="py-6 space-y-6">
      
      <div className="border-b border-surface-200 pb-5">
        <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">Customer Orders</h1>
        <p className="text-surface-500 text-sm mt-0.5 font-medium">Oversee customer transactions, shipping addresses, and status processing updates</p>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="card rounded-2xl border border-surface-200 overflow-hidden shadow-sm bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-surface-50 text-surface-500 border-b border-surface-200">
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Order No</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Date</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Customer</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Final Price</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Status</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Status Modifier</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-100">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-brand-50/30 transition-colors">
                    <td className="p-4 font-bold text-brand-600">{order.orderNumber}</td>
                    <td className="p-4 text-surface-600 font-medium">
                      {new Date(order.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                    </td>
                    <td className="p-4">
                      <span className="font-bold text-surface-900 block">{order.customerName}</span>
                      <span className="text-xs text-surface-500 font-medium">{order.customerEmail}</span>
                    </td>
                    <td className="p-4 font-black text-surface-900">${order.finalAmount.toFixed(2)}</td>
                    <td className="p-4">
                      <span className={`text-[10px] font-extrabold uppercase px-2.5 py-1 rounded tracking-wider ${getStatusColor(order.status)}`}>
                        {order.status}
                      </span>
                    </td>
                    <td className="p-4">
                      {['DELIVERED', 'CANCELLED'].includes(order.status.toUpperCase()) ? (
                        <span className={`inline-block px-3 py-1.5 rounded-lg text-xs font-bold border cursor-not-allowed shadow-sm ${getStatusColor(order.status)}`}>
                          {order.status.toUpperCase()} (LOCKED)
                        </span>
                      ) : (
                        <select
                          value={order.status}
                          onChange={(e) => handleStatusChange(order.id, e.target.value)}
                          className="bg-white border border-surface-200 text-surface-700 font-bold rounded-lg p-1.5 text-xs focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 shadow-sm"
                        >
                          <option value="PENDING">Pending</option>
                          <option value="CONFIRMED">Confirmed</option>
                          <option value="PACKED">Packed</option>
                          <option value="SHIPPED">Shipped</option>
                          <option value="DELIVERED">Delivered</option>
                          <option value="CANCELLED">Cancelled</option>
                        </select>
                      )}
                    </td>
                    <td className="p-4 text-right">
                      <button onClick={() => handleOpenDetails(order)} className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-brand-600 hover:border-brand-200 transition-colors shadow-sm">
                        <Eye className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center space-x-4 py-4 bg-surface-50 border-t border-surface-200">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-surface-900 disabled:opacity-50 shadow-sm"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="text-xs font-bold text-surface-500 uppercase tracking-wider">Page {page + 1} of {totalPages}</span>
              <button
                disabled={page === totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-surface-900 disabled:opacity-50 shadow-sm"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>
      )}

      {/* Details Modal */}
      {showModal && selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-surface-900/40 backdrop-blur-sm p-4">
          <div className="bg-white border border-surface-200 rounded-2xl w-full max-w-xl p-6 shadow-xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            
            <div className="flex justify-between items-center border-b border-surface-100 pb-4">
              <div>
                <h3 className="font-bold text-lg text-surface-900">Order Invoice Details</h3>
                <span className="text-xs text-brand-600 font-bold">{selectedOrder.orderNumber}</span>
              </div>
              <button onClick={handleCloseModal} className="text-surface-400 hover:text-surface-900 bg-surface-50 hover:bg-surface-100 rounded-full p-1 transition-colors">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm text-surface-600">
              <div className="space-y-1 bg-surface-50 p-4 rounded-xl border border-surface-200 shadow-inner">
                <span className="text-[10px] font-bold text-surface-500 uppercase tracking-wider block mb-1">Customer Profile</span>
                <p className="font-bold text-surface-900">{selectedOrder.customerName}</p>
                <p>{selectedOrder.customerEmail}</p>
                <p className="pt-1">Status: <span className={`text-[10px] font-extrabold uppercase px-2 py-0.5 rounded tracking-wider ${getStatusColor(selectedOrder.status)}`}>{selectedOrder.status}</span></p>
              </div>

              <div className="space-y-1 bg-surface-50 p-4 rounded-xl border border-surface-200 shadow-inner">
                <span className="text-[10px] font-bold text-surface-500 uppercase tracking-wider block mb-1">Shipping Destination</span>
                <p className="font-bold text-surface-900">{selectedOrder.shippingAddress.title}</p>
                <p>{selectedOrder.shippingAddress.streetAddress}</p>
                <p>{selectedOrder.shippingAddress.city}, {selectedOrder.shippingAddress.state} {selectedOrder.shippingAddress.zipCode}</p>
                <p>{selectedOrder.shippingAddress.country}</p>
              </div>
            </div>

            {/* Line Items */}
            <div className="space-y-3">
              <span className="text-xs font-bold text-surface-500 uppercase tracking-wider block border-b border-surface-100 pb-2">Line Items</span>
              <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
                {selectedOrder.items.map((item) => (
                  <div key={item.id} className="flex justify-between items-center text-sm bg-white p-3 rounded-xl border border-surface-200 shadow-sm">
                    <div>
                      <span className="font-bold text-surface-900 block">{item.productName}</span>
                      <span className="text-xs text-surface-500 font-medium">Qty: {item.quantity} @ ${item.price.toFixed(2)} each</span>
                    </div>
                    <span className="font-bold text-surface-900">${(item.quantity * item.price).toFixed(2)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Price breakdown */}
            <div className="border-t border-surface-200 pt-4 flex flex-col items-end text-sm space-y-2 font-medium">
              <div className="flex justify-between w-64 text-surface-600">
                <span>Subtotal:</span>
                <span className="text-surface-900">${selectedOrder.totalAmount.toFixed(2)}</span>
              </div>
              {selectedOrder.discountAmount > 0 && (
                <div className="flex justify-between w-64 text-emerald-600">
                  <span>Coupon Discount:</span>
                  <span>-${selectedOrder.discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between w-64 border-t border-surface-200 pt-3 font-black text-base text-surface-900">
                <span>Total Amount Paid:</span>
                <span>${selectedOrder.finalAmount.toFixed(2)}</span>
              </div>
            </div>

          </div>
        </div>
      )}

    </div>
  )
}

export default AdminOrders
