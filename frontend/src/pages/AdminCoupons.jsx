import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { Plus, Edit2, Trash2, X, Tag } from 'lucide-react'

const AdminCoupons = () => {
  const [coupons, setCoupons] = useState([])
  const [loading, setLoading] = useState(true)

  // Modals state
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId] = useState(null)

  // Fields state
  const [code, setCode] = useState('')
  const [discountPercentage, setDiscountPercentage] = useState('')
  const [maxDiscount, setMaxDiscount] = useState('')
  const [minimumOrderAmount, setMinimumOrderAmount] = useState('')
  const [expiryDate, setExpiryDate] = useState('')
  const [active, setActive] = useState(true)

  const loadCoupons = async () => {
    setLoading(true)
    try {
      const res = await API.get('/admin/coupons')
      setCoupons(res.data)
    } catch (err) {
      console.error('Failed to load coupons list', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCoupons()
  }, [])

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    
    if (new Date(expiryDate) < new Date()) {
      toast.error('Expiration date cannot be in the past')
      return
    }

    try {
      const payload = {
        code,
        discountPercentage: parseInt(discountPercentage),
        maxDiscount: parseFloat(maxDiscount),
        minimumOrderAmount: parseFloat(minimumOrderAmount),
        expiryDate: new Date(expiryDate).toISOString(),
        active
      }

      if (editId) {
        await API.put(`/admin/coupons/${editId}`, payload)
      } else {
        await API.post('/admin/coupons', payload)
      }

      handleCloseModal()
      loadCoupons()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save coupon details')
    }
  }

  const handleEditClick = (c) => {
    setEditId(c.id)
    setCode(c.code)
    setDiscountPercentage(c.discountPercentage)
    setMaxDiscount(c.maxDiscount)
    setMinimumOrderAmount(c.minimumOrderAmount)
    // Convert ISO to local input datetime-local string format
    const localDate = new Date(c.expiryDate)
    const formatted = new Date(localDate.getTime() - localDate.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
    setExpiryDate(formatted)
    setActive(c.active)
    setShowModal(true)
  }

  const handleDeleteClick = async (id) => {
    if (!window.confirm('Delete this coupon?')) return
    try {
      await API.delete(`/admin/coupons/${id}`)
      loadCoupons()
    } catch (err) {
      toast.error('Failed to delete coupon')
    }
  }

  const handleOpenAddModal = () => {
    setEditId(null)
    setCode('')
    setDiscountPercentage('')
    setMaxDiscount('')
    setMinimumOrderAmount('')
    setExpiryDate('')
    setActive(true)
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setEditId(null)
    setShowModal(false)
  }

  return (
    <div className="py-6 space-y-6">
      
      <div className="flex justify-between items-center border-b border-surface-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-surface-200 tracking-tight">Discount Coupons</h1>
          <p className="text-surface-500 text-sm mt-0.5 font-medium">Configure and monitor customer promotional discount coupons</p>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="btn-primary py-2.5 px-4 text-sm flex items-center space-x-1.5"
        >
          <Plus className="h-4.5 w-4.5" />
          <span>New Coupon</span>
        </button>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {coupons.map((coupon) => (
            <div key={coupon.id} className="card p-6 rounded-2xl border border-surface-200 flex flex-col justify-between h-48 shadow-sm bg-white relative overflow-hidden group hover:border-brand-300 transition-colors">
              <div className="absolute top-0 right-0 w-24 h-24 bg-brand-50 rounded-full blur-2xl pointer-events-none group-hover:bg-brand-100 transition-colors"></div>
              
              <div className="relative z-10">
                <div className="flex justify-between items-start">
                  <span className="font-extrabold text-brand-700 bg-brand-50 border border-brand-200 px-3 py-1 rounded-md text-sm uppercase tracking-widest flex items-center space-x-1.5 shadow-sm">
                    <Tag className="h-4 w-4 text-brand-500" />
                    <span>{coupon.code}</span>
                  </span>
                  
                  <span className={`px-2.5 py-1 rounded tracking-wider text-[10px] font-extrabold uppercase ${
                    coupon.active && new Date(coupon.expiryDate) > new Date()
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}>
                    {coupon.active && new Date(coupon.expiryDate) > new Date() ? 'ACTIVE' : 'EXPIRED / INACTIVE'}
                  </span>
                </div>

                <div className="mt-5 space-y-1.5 text-sm text-surface-600 font-medium">
                  <p>Discount: <span className="text-surface-900 font-bold">{coupon.discountPercentage}% Off</span> (Max ${coupon.maxDiscount})</p>
                  <p>Min Order: <span className="text-surface-900">${coupon.minimumOrderAmount}</span></p>
                  <p>Expiry: <span className="text-surface-900">{new Date(coupon.expiryDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span></p>
                </div>
              </div>

              <div className="flex justify-end space-x-2 pt-4 border-t border-surface-100 relative z-10">
                <button
                  onClick={() => handleEditClick(coupon)}
                  className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-brand-600 hover:border-brand-200 transition-colors shadow-sm"
                >
                  <Edit2 className="h-4 w-4" />
                </button>
                <button
                  onClick={() => handleDeleteClick(coupon.id)}
                  className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors shadow-sm"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Form */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-surface-900/40 backdrop-blur-sm p-4">
          <div className="bg-white border border-surface-200 rounded-2xl w-full max-w-md p-6 shadow-xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            
            <div className="flex justify-between items-center border-b border-surface-100 pb-4">
              <h3 className="font-bold text-lg text-surface-900">{editId ? 'Edit Coupon' : 'Create Coupon'}</h3>
              <button onClick={handleCloseModal} className="text-surface-400 hover:text-surface-900 bg-surface-50 hover:bg-surface-100 rounded-full p-1 transition-colors focus:outline-none">
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Coupon Code</label>
                  <input
                    type="text"
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="MEGA50"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Discount (%)</label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    required
                    value={discountPercentage}
                    onChange={(e) => setDiscountPercentage(e.target.value)}
                    placeholder="20"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Max Cap Discount ($)</label>
                  <input
                    type="number"
                    required
                    value={maxDiscount}
                    onChange={(e) => setMaxDiscount(e.target.value)}
                    placeholder="50"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Min Purchase Order ($)</label>
                  <input
                    type="number"
                    required
                    value={minimumOrderAmount}
                    onChange={(e) => setMinimumOrderAmount(e.target.value)}
                    placeholder="100"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Expiration Date & Time</label>
                <input
                  type="datetime-local"
                  required
                  value={expiryDate}
                  onChange={(e) => setExpiryDate(e.target.value)}
                  className="input-field py-2.5 text-sm"
                />
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <input
                  type="checkbox"
                  id="couponActive"
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  className="rounded text-brand-600 focus:ring-brand-500 bg-white border-surface-300 h-4 w-4 transition-colors"
                />
                <label htmlFor="couponActive" className="text-sm font-bold text-surface-700 select-none">Active for use</label>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-surface-200 mt-2">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="btn-secondary py-2"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary py-2"
                >
                  Save Coupon
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  )
}

export default AdminCoupons
