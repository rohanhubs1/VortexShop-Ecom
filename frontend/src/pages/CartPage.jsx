import React, { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { getImageUrl } from '../utils/imageHelper'
import { Trash2, ShoppingBag, ArrowRight, Minus, Plus, Tag } from 'lucide-react'
import API from '../api/axios'

const CartPage = () => {
  const { cart, cartLoading, updateCartQuantity, removeFromCart } = useCart()
  const { isAdmin } = useAuth()
  const [couponCode, setCouponCode] = useState('')
  const [discountInfo, setDiscountInfo] = useState(null)
  const [couponError, setCouponError] = useState('')
  const [couponLoading, setCouponLoading] = useState(false)
  const [availableCoupons, setAvailableCoupons] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    API.get('/coupons').then(res => setAvailableCoupons(res.data)).catch(err => console.error(err))
  }, [])

  const handleApplyCoupon = async (e) => {
    e.preventDefault()
    setCouponError('')
    setDiscountInfo(null)
    if (!couponCode.trim()) return

    setCouponLoading(true)
    try {
      const res = await API.post(`/coupons/validate`, { code: couponCode.trim() })
      const found = res.data

      if (cart.totalAmount < found.minimumOrderAmount) {
        throw new Error(`Minimum order amount of $${found.minimumOrderAmount} required for this coupon.`)
      }

      const calculatedDiscount = (cart.totalAmount * found.discountPercentage) / 100
      const actualDiscount = Math.min(calculatedDiscount, found.maxDiscount)

      setDiscountInfo({
        code: found.code,
        discountPercentage: found.discountPercentage,
        discountAmount: actualDiscount
      })
    } catch (err) {
      setCouponError(err.response?.data?.message || err.message || 'Invalid coupon code')
    } finally {
      setCouponLoading(false)
    }
  }

  const handleCheckout = () => {
    navigate('/checkout', {
      state: {
        appliedCoupon: discountInfo ? discountInfo.code : null,
        discountAmount: discountInfo ? discountInfo.discountAmount : 0
      }
    })
  }

  if (cartLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  const isEmpty = !cart || !cart.items || cart.items.length === 0

  return (
    <div className="py-8 space-y-8 max-w-7xl mx-auto">
      <div>
        <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">Shopping Cart</h1>
        <p className="text-surface-500 text-sm mt-1">Review items in your active shopping session</p>
      </div>

      {isEmpty ? (
        <div className="card rounded-2xl p-16 text-center space-y-4 max-w-2xl mx-auto border-dashed border-2 border-surface-200 bg-surface-50">
          <span className="text-6xl select-none">🛒</span>
          <h2 className="text-2xl font-bold text-surface-900">Your Cart is Empty</h2>
          <p className="text-surface-500 text-sm">Add tech hardware, learning books, or decor to begin checkout.</p>
          <Link to="/catalog" className="btn-primary inline-block px-8">
            Start Shopping
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          
          {/* Items Table */}
          <div className="lg:col-span-2 space-y-4">
            {cart.items.map((item) => (
              <div key={item.id} className="bg-white border border-surface-200 rounded-2xl p-5 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-sm">
                
                <div className="flex items-center space-x-4 flex-1">
                  <div className="h-20 w-20 bg-surface-50 rounded-xl flex items-center justify-center shrink-0 border border-surface-100 overflow-hidden p-2">
                     <img src={getImageUrl(item.productImageUrl)} alt={item.productName} className="h-full object-contain mix-blend-multiply" />
                  </div>
                  <div>
                    <Link to={`/products/${item.productId}`} className="font-bold text-base text-surface-900 hover:text-brand-600 transition-colors line-clamp-2">
                      {item.productName}
                    </Link>
                    <span className="text-sm font-semibold text-brand-600 mt-1 block">${item.productPrice.toFixed(2)}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end w-full sm:w-auto space-x-6">
                  {/* Quantity controls */}
                  <div className="flex items-center bg-white border border-surface-300 rounded-lg">
                    <button
                      onClick={() => {
                        if (item.quantity === 1) {
                          removeFromCart(item.productId).catch(err => toast.error(err))
                        } else {
                          updateCartQuantity(item.productId, item.quantity - 1).catch(err => toast.error(err))
                        }
                      }}
                      className="p-2 text-surface-500 hover:text-surface-900 transition-colors"
                    >
                      <Minus className="h-4 w-4" />
                    </button>
                    <span className="px-3 text-sm font-bold text-surface-900">{item.quantity}</span>
                    <button
                      disabled={item.quantity >= item.stockQuantity}
                      onClick={() => {
                        updateCartQuantity(item.productId, item.quantity + 1).catch(err => toast.error(err))
                      }}
                      className="p-2 text-surface-500 hover:text-surface-900 disabled:opacity-30"
                    >
                      <Plus className="h-4 w-4" />
                    </button>
                  </div>

                  <span className="text-lg font-bold text-surface-900 w-24 text-right">${item.subTotal.toFixed(2)}</span>

                  <button
                    onClick={() => {
                      removeFromCart(item.productId).catch(err => toast.error(err))
                    }}
                    className="p-2 text-surface-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <Trash2 className="h-5 w-5" />
                  </button>
                </div>

              </div>
            ))}
          </div>

          {/* Cart Summary Panel */}
          <div className="card p-6 rounded-2xl sticky top-24">
            <h3 className="font-bold text-xl text-surface-900 border-b border-surface-100 pb-4 mb-4">Order Summary</h3>
            
            <div className="space-y-4 text-sm font-medium">
              <div className="flex justify-between text-surface-600">
                <span>Subtotal:</span>
                <span className="text-surface-900">${cart.totalAmount.toFixed(2)}</span>
              </div>

              {discountInfo && (
                <div className="flex justify-between text-emerald-600">
                  <span>Discount ({discountInfo.code}):</span>
                  <span>-${discountInfo.discountAmount.toFixed(2)}</span>
                </div>
              )}

              <div className="flex justify-between text-surface-600">
                <span>Shipping:</span>
                <span className="text-emerald-600 font-semibold">FREE</span>
              </div>

              <div className="border-t border-surface-200 pt-4 flex justify-between font-extrabold text-lg text-surface-900">
                <span>Total Amount:</span>
                <span>
                  ${discountInfo ? (cart.totalAmount - discountInfo.discountAmount).toFixed(2) : cart.totalAmount.toFixed(2)}
                </span>
              </div>
            </div>

            {/* Coupon field */}
            <form onSubmit={handleApplyCoupon} className="space-y-4 mt-6">
              <div>
                <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-2">Discount Code</label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      placeholder="Enter code..."
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value)}
                      className="w-full bg-surface-50 border border-surface-300 rounded-lg py-2.5 pl-9 pr-3 text-sm text-surface-900 uppercase focus:outline-none focus:border-brand-500 focus:bg-white transition-colors"
                    />
                    <Tag className="absolute left-3 top-3 h-4 w-4 text-surface-400" />
                  </div>
                  <button
                    type="submit"
                    disabled={couponLoading}
                    className="bg-surface-800 hover:bg-surface-900 text-white font-semibold text-sm px-4 py-2.5 rounded-lg transition-colors shadow-sm"
                  >
                    Apply
                  </button>
                </div>
                {couponError && <p className="text-red-500 text-xs mt-2 font-medium">{couponError}</p>}
                {discountInfo && <p className="text-emerald-600 text-xs mt-2 font-bold flex items-center"><Tag className="w-3 h-3 mr-1" /> Promo Applied: {discountInfo.discountPercentage}% off!</p>}
              </div>

              {/* Available Coupons */}
              {availableCoupons.length > 0 && (
                <div className="space-y-2 mt-4 bg-surface-50 p-4 rounded-xl border border-surface-100">
                  <span className="text-[10px] uppercase font-bold text-surface-500 tracking-wider block mb-2">Available Offers</span>
                  <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
                    {availableCoupons.map(c => (
                      <div key={c.id} className="bg-white border border-dashed border-brand-300 rounded-lg p-3 flex justify-between items-center cursor-pointer hover:border-brand-500 transition-colors shadow-sm" onClick={() => setCouponCode(c.code)}>
                        <div>
                          <span className="font-bold text-brand-700 text-sm block">{c.code}</span>
                          <span className="text-[10px] text-surface-500 font-medium block">Min Spend: ${c.minimumOrderAmount}</span>
                        </div>
                        <span className="bg-brand-50 text-brand-700 px-2 py-1 rounded text-[10px] font-bold uppercase">
                          {c.discountPercentage}% OFF
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </form>

            {isAdmin && (
              <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl text-sm font-medium mt-6 mb-2">
                Admin accounts cannot place orders. Please use a customer account.
              </div>
            )}

            <button
              onClick={handleCheckout}
              disabled={isAdmin}
              className="w-full btn-primary mt-6 py-3.5 flex items-center justify-center space-x-2 text-base disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-surface-300 disabled:text-surface-500"
            >
              <span>Proceed to Checkout</span>
              <ArrowRight className="h-5 w-5" />
            </button>

          </div>

        </div>
      )}

    </div>
  )
}

export default CartPage
