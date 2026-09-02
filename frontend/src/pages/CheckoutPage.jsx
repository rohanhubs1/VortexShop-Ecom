import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import API from '../api/axios'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { Plus, Check, MapPin, ChevronLeft, ShieldCheck, CreditCard } from 'lucide-react'
import vortexQr from '../assets/vortex-qr.png';

const CheckoutPage = () => {
  const { cart, fetchCart } = useCart()
  const { isAdmin } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  // Coupon parameters passed from Cart page
  const appliedCoupon = location.state?.appliedCoupon || null
  const discountAmount = location.state?.discountAmount || 0

  // State
  const [addresses, setAddresses] = useState([])
  const [selectedAddressId, setSelectedAddressId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [placingOrder, setPlacingOrder] = useState(false)
  const [error, setError] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('card')

  // Add Address Inline state
  const [showAddForm, setShowAddForm] = useState(false)
  const [title, setTitle] = useState('')
  const [streetAddress, setStreetAddress] = useState('')
  const [city, setCity] = useState('')
  const [state, setState] = useState('')
  const [zipCode, setZipCode] = useState('')
  const [country, setCountry] = useState('United States')
  const [isDefault, setIsDefault] = useState(false)

  const loadAddresses = async () => {
    try {
      const res = await API.get('/users/profile/addresses')
      setAddresses(res.data)
      const defaultAddr = res.data.find(a => a.isDefault)
      if (defaultAddr) {
        setSelectedAddressId(defaultAddr.id)
      } else if (res.data.length > 0) {
        setSelectedAddressId(res.data[0].id)
      }
    } catch (err) {
      console.error('Failed to load user address book', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAddresses()
  }, [])

  const handleAddAddress = async (e) => {
    e.preventDefault()
    try {
      const res = await API.post('/users/profile/addresses', {
        title, streetAddress, city, state, zipCode, country, isDefault
      })
      
      setTitle('')
      setStreetAddress('')
      setCity('')
      setState('')
      setZipCode('')
      setIsDefault(false)
      setShowAddForm(false)
      
      const list = await API.get('/users/profile/addresses')
      setAddresses(list.data)
      setSelectedAddressId(res.data.id)
    } catch (err) {
      toast.error('Failed to add address')
    }
  }

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      setError('Please select a shipping address.')
      return
    }

    setPlacingOrder(true)
    setError('')
    try {
      await API.post('/orders', {
        shippingAddressId: selectedAddressId,
        couponCode: appliedCoupon
      })
      await fetchCart()
      navigate('/order-success', { state: { orderSuccess: true } })
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to process transaction.')
    } finally {
      setPlacingOrder(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  const subTotal = cart?.totalAmount || 0
  const totalAmount = subTotal - discountAmount

  return (
    <div className="py-8 space-y-8 max-w-6xl mx-auto">
      
      {/* Header */}
      <div className="flex items-center space-x-3 border-b border-surface-200 pb-6">
        <Link to="/cart" className="p-2 bg-white border border-surface-300 rounded-xl text-surface-600 hover:text-brand-600 hover:border-brand-300 shadow-sm transition-colors">
          <ChevronLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-3xl font-extrabold text-surface-300 tracking-tight">Checkout</h1>
          <p className="text-surface-500 text-sm mt-1">Select a shipping address and complete your transaction</p>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm font-medium">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        
        {/* Left Side: Address Selection & Form */}
        <div className="lg:col-span-2 space-y-6">
          
          <div className="card p-6 rounded-2xl">
            <div className="flex justify-between items-center border-b border-surface-100 pb-4 mb-4">
              <h3 className="font-bold text-lg text-surface-900 flex items-center space-x-2">
                <MapPin className="h-5 w-5 text-brand-600" />
                <span>Shipping Address</span>
              </h3>
              {!showAddForm && !isAdmin && (
                <button
                  onClick={() => setShowAddForm(true)}
                  className="text-sm bg-brand-50 text-brand-700 hover:bg-brand-100 px-3 py-1.5 rounded-lg flex items-center space-x-1 font-semibold transition-colors"
                >
                  <Plus className="h-4 w-4" />
                  <span>Add Address</span>
                </button>
              )}
            </div>

            {/* Address Cards */}
            {!showAddForm && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {addresses.length === 0 ? (
                  <p className="text-surface-500 text-sm italic py-4 col-span-2">No shipping addresses configured. Please add one below.</p>
                ) : (
                  addresses.map((addr) => (
                    <div
                      key={addr.id}
                      onClick={() => setSelectedAddressId(addr.id)}
                      className={`cursor-pointer rounded-xl p-5 border-2 text-sm flex flex-col justify-between h-40 relative transition-all shadow-sm ${
                        selectedAddressId === addr.id
                          ? 'bg-brand-50/50 border-brand-500 shadow-brand-500/10'
                          : 'bg-white border-surface-200 hover:border-surface-300'
                      }`}
                    >
                      <div>
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-bold text-surface-900">{addr.title}</span>
                          {addr.isDefault && <span className="bg-surface-100 text-surface-600 px-2 py-0.5 rounded text-[10px] font-bold tracking-wider">DEFAULT</span>}
                        </div>
                        <p className="text-surface-600 line-clamp-2 leading-relaxed">{addr.streetAddress}</p>
                        <p className="text-surface-600">{addr.city}, {addr.state} {addr.zipCode}</p>
                      </div>
                      
                      {selectedAddressId === addr.id && (
                        <div className="absolute bottom-4 right-4 bg-brand-500 text-white rounded-full p-1 shadow-md">
                          <Check className="h-4 w-4" />
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}

            {/* Inline Add Address Form */}
            {showAddForm && (
              <form onSubmit={handleAddAddress} className="space-y-4 border border-surface-200 p-6 rounded-xl bg-surface-50 shadow-inner">
                <h4 className="font-bold text-base text-surface-900 border-b border-surface-200 pb-2 mb-4">Add New Address</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase mb-1">Address Label</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Home, Office"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      className="input-field py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase mb-1">Street Address</label>
                    <input
                      type="text"
                      required
                      placeholder="123 Dev Lane"
                      value={streetAddress}
                      onChange={(e) => setStreetAddress(e.target.value)}
                      className="input-field py-2 text-sm"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase mb-1">City</label>
                    <input
                      type="text"
                      required
                      placeholder="San Jose"
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                      className="input-field py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase mb-1">State</label>
                    <input
                      type="text"
                      required
                      placeholder="CA"
                      value={state}
                      onChange={(e) => setState(e.target.value)}
                      className="input-field py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase mb-1">Zip Code</label>
                    <input
                      type="text"
                      required
                      placeholder="95112"
                      value={zipCode}
                      onChange={(e) => setZipCode(e.target.value)}
                      className="input-field py-2 text-sm"
                    />
                  </div>
                </div>

                <div className="flex items-center space-x-2 pt-3">
                  <input
                    type="checkbox"
                    id="isDefault"
                    checked={isDefault}
                    onChange={(e) => setIsDefault(e.target.checked)}
                    className="rounded border-surface-300 text-brand-600 focus:ring-brand-500 h-4 w-4"
                  />
                  <label htmlFor="isDefault" className="text-sm font-medium text-surface-700 select-none">Set as default shipping address</label>
                </div>

                <div className="flex justify-end space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowAddForm(false)}
                    className="btn-secondary py-2"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-primary py-2"
                  >
                    Save & Select
                  </button>
                </div>
              </form>
            )}

          </div>

          <div className="card p-6 rounded-2xl">
            <div className="flex justify-between items-center border-b border-surface-100 pb-4 mb-4">
              <h3 className="font-bold text-lg text-surface-900 flex items-center space-x-2">
                <CreditCard className="h-5 w-5 text-brand-600" />
                <span>Payment Method</span>
              </h3>
              <div className="flex space-x-1.5">
                <div className="h-6 w-10 bg-blue-100 border border-blue-200 rounded flex items-center justify-center text-[8px] font-bold text-blue-800">VISA</div>
                <div className="h-6 w-10 bg-red-100 border border-red-200 rounded flex items-center justify-center text-[8px] font-bold text-red-800">MC</div>
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
              {[
                { id: 'card', label: 'Credit Card', icon: '💳' },
                { id: 'upi', label: 'UPI / QR', icon: '📱' },
                { id: 'netbanking', label: 'Net Banking', icon: '🏦' },
                { id: 'cod', label: 'Cash on Delivery', icon: '💵' }
              ].map(method => (
                <div
                  key={method.id}
                  onClick={() => setPaymentMethod(method.id)}
                  className={`cursor-pointer rounded-xl p-3 border text-center transition-all flex flex-col items-center justify-center space-y-2 ${
                    paymentMethod === method.id
                      ? 'bg-brand-50 border-brand-500 text-brand-700 shadow-sm'
                      : 'bg-white border-surface-200 text-surface-600 hover:border-surface-300 hover:bg-surface-50'
                  }`}
                >
                  <span className="text-2xl mb-1 block">{method.icon}</span>
                  <span className="text-[10px] font-bold uppercase tracking-wider">{method.label}</span>
                </div>
              ))}
            </div>
            
            {paymentMethod === 'card' && (
              <div className="bg-surface-50 border border-surface-200 rounded-xl p-5 space-y-4">
                <div>
                  <label className="block text-[10px] font-bold text-surface-500 uppercase mb-1">Card Number</label>
                  <div className="relative">
                    <input
                      type="text"
                      placeholder="4000 1234 5678 9010"
                      maxLength="19"
                      className="input-field py-2.5 pl-10 font-mono tracking-widest text-sm"
                    />
                    <CreditCard className="absolute left-3 top-3 h-5 w-5 text-surface-400" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-bold text-surface-500 uppercase mb-1">Expiry Date</label>
                    <input
                      type="text"
                      placeholder="MM/YY"
                      maxLength="5"
                      className="input-field py-2.5 font-mono text-center text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-surface-500 uppercase mb-1">CVC</label>
                    <input
                      type="password"
                      placeholder="123"
                      maxLength="4"
                      className="input-field py-2.5 font-mono text-center text-sm"
                    />
                  </div>
                </div>
                
                <div className="flex items-center space-x-2 text-[11px] font-medium text-brand-700 bg-brand-50 p-3 rounded-lg border border-brand-100">
                  <ShieldCheck className="h-4 w-4 shrink-0 text-brand-600" />
                  <span>Simulation Mode: Any valid card format will be accepted. No real charges are made.</span>
                </div>
              </div>
            )}

            {paymentMethod === 'upi' && (
              <div className="bg-surface-50 border border-surface-200 rounded-xl p-5 text-center space-y-3">
                <div className="mx-auto w-32 h-32 bg-white rounded-xl p-2 border border-surface-200 flex items-center justify-center">
                  <img src={vortexQr} alt="UPI QR" className="w-full h-full opacity-80" />
                </div>
                <p className="text-sm text-surface-600 font-medium">Scan QR Code using any UPI App</p>
                <div className="flex items-center space-x-2 text-[11px] font-medium text-brand-700 bg-brand-50 p-3 rounded-lg border border-brand-100 justify-center">
                  <ShieldCheck className="h-4 w-4 shrink-0 text-brand-600" />
                  <span>Simulation Mode: Order will proceed without real scan.</span>
                </div>
              </div>
            )}

            {paymentMethod === 'netbanking' && (
              <div className="bg-surface-50 border border-surface-200 rounded-xl p-5 space-y-4">
                <label className="block text-[10px] font-bold text-surface-500 uppercase mb-1">Select Bank</label>
                <select className="input-field py-2.5 text-sm">
                  <option>HDFC Bank</option>
                  <option>State Bank of India</option>
                  <option>ICICI Bank</option>
                  <option>Axis Bank</option>
                  <option>Kotak Mahindra Bank</option>
                </select>
                <div className="flex items-center space-x-2 text-[11px] font-medium text-brand-700 bg-brand-50 p-3 rounded-lg border border-brand-100">
                  <ShieldCheck className="h-4 w-4 shrink-0 text-brand-600" />
                  <span>Simulation Mode: Bank redirects will be bypassed.</span>
                </div>
              </div>
            )}

            {paymentMethod === 'cod' && (
              <div className="bg-surface-50 border border-surface-200 rounded-xl p-5 flex items-center space-x-4">
                <div className="h-12 w-12 bg-white border border-surface-200 rounded-full flex items-center justify-center shrink-0 shadow-sm">
                  <span className="text-2xl">💵</span>
                </div>
                <div>
                  <h4 className="font-bold text-sm text-surface-900">Cash on Delivery</h4>
                  <p className="text-xs text-surface-500 mt-1">Pay with cash or UPI when the order is delivered to your doorstep.</p>
                </div>
              </div>
            )}

          </div>

        </div>

        {/* Right Side: Summary & Action */}
        <div className="card p-6 rounded-2xl sticky top-24">
          <h3 className="font-bold text-xl text-surface-900 border-b border-surface-100 pb-4 mb-4">Order Review</h3>

          <div className="space-y-4 max-h-60 overflow-y-auto pr-2 mb-4">
            {cart?.items?.map((item) => (
              <div key={item.id} className="flex justify-between items-center">
                <div>
                  <span className="font-bold text-sm text-surface-900 block truncate max-w-[180px]">{item.productName}</span>
                  <span className="text-xs text-surface-500">Qty: {item.quantity}</span>
                </div>
                <span className="font-bold text-sm text-surface-900">${item.subTotal.toFixed(2)}</span>
              </div>
            ))}
          </div>

          <div className="border-t border-surface-200 pt-4 space-y-3 text-sm font-medium">
            <div className="flex justify-between text-surface-600">
              <span>Subtotal:</span>
              <span className="text-surface-900">${subTotal.toFixed(2)}</span>
            </div>
            
            {appliedCoupon && (
              <div className="flex justify-between text-emerald-600">
                <span>Coupon ({appliedCoupon}):</span>
                <span>-${discountAmount.toFixed(2)}</span>
              </div>
            )}

            <div className="flex justify-between text-surface-600">
              <span>Shipping Fee:</span>
              <span className="text-emerald-600 font-semibold">FREE</span>
            </div>

            <div className="border-t border-surface-200 pt-4 flex justify-between font-black text-xl text-surface-900">
              <span>Order Total:</span>
              <span>${totalAmount.toFixed(2)}</span>
            </div>
          </div>

          {isAdmin && (
            <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl text-sm font-medium mt-6">
              Admin accounts cannot place orders. Please use a customer account.
            </div>
          )}

          <button
            onClick={handlePlaceOrder}
            disabled={placingOrder || !selectedAddressId || isAdmin}
            className="w-full btn-primary mt-6 py-4 text-base flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-surface-300 disabled:text-surface-500"
          >
            <span>{placingOrder ? 'Processing...' : 'Place Order'}</span>
          </button>

          <div className="flex justify-center items-center space-x-1.5 text-xs text-surface-500 font-medium mt-4">
            <ShieldCheck className="h-4 w-4 text-emerald-500" />
            <span>Secure checkout guaranteed.</span>
          </div>

        </div>

      </div>

    </div>
  )
}

export default CheckoutPage
