import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import API from '../api/axios'
import { useAuth } from '../context/AuthContext'
import { Edit2, Trash2, Plus, Package, ChevronRight } from 'lucide-react'

const ProfilePage = () => {
  const { user, setUser, isAdmin } = useAuth()

  // State
  const [firstName, setFirstName] = useState(user?.firstName || '')
  const [lastName, setLastName] = useState(user?.lastName || '')
  const [phone, setPhone] = useState(user?.phone || '')
  const [profileSuccess, setProfileSuccess] = useState(false)
  const [profileError, setProfileError] = useState('')

  const [addresses, setAddresses] = useState([])
  const [addressesLoading, setAddressesLoading] = useState(true)

  const [recentOrders, setRecentOrders] = useState([])
  const [ordersLoading, setOrdersLoading] = useState(true)

  // Address edit state
  const [showForm, setShowForm] = useState(false)
  const [editId, setEditId] = useState(null)
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
    } catch (err) {
      console.error('Failed to load address list', err)
    } finally {
      setAddressesLoading(false)
    }
  }

  const loadRecentOrders = async () => {
    try {
      const res = await API.get('/orders?size=2')
      setRecentOrders(res.data.content || [])
    } catch (err) {
      console.error('Failed to load recent orders', err)
    } finally {
      setOrdersLoading(false)
    }
  }

  useEffect(() => {
    loadAddresses()
    loadRecentOrders()
  }, [])

  const handleUpdateProfile = async (e) => {
    e.preventDefault()
    setProfileSuccess(false)
    setProfileError('')
    try {
      const res = await API.put('/users/profile', {
        firstName, lastName, phone, email: user.email, password: 'password123' // default bypass password requirement
      })
      setUser(res.data)
      setProfileSuccess(true)
    } catch (err) {
      setProfileError(err.response?.data?.message || 'Failed to update profile')
    }
  }

  const handleAddressSubmit = async (e) => {
    e.preventDefault()
    try {
      const payload = { title, streetAddress, city, state, zipCode, country, isDefault }
      if (editId) {
        await API.put(`/users/profile/addresses/${editId}`, payload)
      } else {
        await API.post('/users/profile/addresses', payload)
      }
      // Reset form
      handleCancelForm()
      // Reload
      loadAddresses()
    } catch (err) {
      toast.error('Failed to save address')
    }
  }

  const handleEditAddress = (addr) => {
    setEditId(addr.id)
    setTitle(addr.title)
    setStreetAddress(addr.streetAddress)
    setCity(addr.city)
    setState(addr.state)
    setZipCode(addr.zipCode)
    setCountry(addr.country)
    setIsDefault(addr.isDefault)
    setShowForm(true)
  }

  const handleDeleteAddress = async (id) => {
    if (!window.confirm('Delete this address?')) return
    try {
      await API.delete(`/users/profile/addresses/${id}`)
      loadAddresses()
    } catch (err) {
      toast.error('Failed to delete address')
    }
  }

  const handleCancelForm = () => {
    setEditId(null)
    setTitle('')
    setStreetAddress('')
    setCity('')
    setState('')
    setZipCode('')
    setIsDefault(false)
    setShowForm(false)
  }

  return (
    <div className="py-8 space-y-8 max-w-6xl mx-auto">
      <div className="border-b border-surface-200 pb-6">
        <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">My Profile</h1>
        <p className="text-surface-500 text-sm mt-1">Manage your account settings, contact information, and address book</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        
        {/* Profile Card */}
        <div className="card p-6 rounded-2xl bg-white shadow-sm border-t-4 border-t-brand-500">
          <div className="text-center pb-6 border-b border-surface-100 mb-6">
            <div className="mx-auto h-20 w-20 bg-brand-50 border border-brand-100 rounded-full flex items-center justify-center text-brand-600 font-black text-3xl mb-3 shadow-inner">
              {user?.firstName[0]}
            </div>
            <h2 className="text-xl font-bold text-surface-900">{user?.firstName} {user?.lastName}</h2>
            <span className="text-xs font-bold text-surface-500 uppercase tracking-wider mt-1 block">{user?.roles?.includes('ROLE_ADMIN') ? 'Administrator' : 'Customer'}</span>
          </div>

          <form onSubmit={handleUpdateProfile} className="space-y-5">
            {profileSuccess && (
              <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 p-3 rounded-lg text-xs text-center font-bold">
                Profile updated successfully!
              </div>
            )}
            {profileError && (
              <div className="bg-red-50 border border-red-200 text-red-700 p-3 rounded-lg text-xs text-center font-bold">
                {profileError}
              </div>
            )}

            <div className="space-y-1">
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">First Name</label>
              <input
                type="text"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="input-field py-2.5 text-sm"
              />
            </div>

            <div className="space-y-1">
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Last Name</label>
              <input
                type="text"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="input-field py-2.5 text-sm"
              />
            </div>

            <div className="space-y-1">
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Email Address</label>
              <input
                type="email"
                disabled
                value={user?.email}
                className="w-full bg-surface-100 border border-surface-200 rounded-lg p-2.5 text-sm text-surface-500 cursor-not-allowed font-medium"
              />
            </div>

            <div className="space-y-1">
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Mobile Phone</label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="input-field py-2.5 text-sm"
              />
            </div>

            <button
              type="submit"
              className="w-full btn-primary py-3 text-sm mt-4"
            >
              Update Settings
            </button>
          </form>
        </div>

        {/* Addresses Management */}
        <div className="lg:col-span-2 space-y-6">
          
          <div className="card p-6 rounded-2xl shadow-sm">
            
            <div className="flex justify-between items-center border-b border-surface-100 pb-4 mb-4">
              <div>
                <h3 className="font-bold text-xl text-surface-900">Address Book</h3>
                <p className="text-xs font-medium text-surface-500 mt-0.5">Configure delivery locations for faster checkouts</p>
              </div>
              {!showForm && !isAdmin && (
                <button
                  onClick={() => setShowForm(true)}
                  className="btn-secondary py-2 px-3 text-xs flex items-center space-x-1"
                >
                  <Plus className="h-4 w-4" />
                  <span>New Address</span>
                </button>
              )}
            </div>

            {/* Address Form */}
            {showForm && (
              <form onSubmit={handleAddressSubmit} className="space-y-4 border border-surface-200 p-6 rounded-xl bg-surface-50 mb-6 shadow-inner">
                <h4 className="font-bold text-base text-surface-900 border-b border-surface-200 pb-2 mb-4">{editId ? 'Edit Address' : 'New Address'}</h4>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-1">Label</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Home, Office"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      className="input-field py-2.5 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-1">Street Address</label>
                    <input
                      type="text"
                      required
                      placeholder="123 Dev Lane"
                      value={streetAddress}
                      onChange={(e) => setStreetAddress(e.target.value)}
                      className="input-field py-2.5 text-sm"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-1">City</label>
                    <input
                      type="text"
                      required
                      placeholder="San Jose"
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                      className="input-field py-2.5 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-1">State</label>
                    <input
                      type="text"
                      required
                      placeholder="CA"
                      value={state}
                      onChange={(e) => setState(e.target.value)}
                      className="input-field py-2.5 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-surface-500 uppercase tracking-wider mb-1">Zip Code</label>
                    <input
                      type="text"
                      required
                      placeholder="95112"
                      value={zipCode}
                      onChange={(e) => setZipCode(e.target.value)}
                      className="input-field py-2.5 text-sm"
                    />
                  </div>
                </div>

                <div className="flex items-center space-x-2 pt-3">
                  <input
                    type="checkbox"
                    id="profileIsDefault"
                    checked={isDefault}
                    onChange={(e) => setIsDefault(e.target.checked)}
                    className="rounded border-surface-300 text-brand-600 focus:ring-brand-500 h-4 w-4"
                  />
                  <label htmlFor="profileIsDefault" className="text-sm font-medium text-surface-700 select-none">Set as default shipping address</label>
                </div>

                <div className="flex justify-end space-x-3 pt-4 border-t border-surface-200 mt-2">
                  <button
                    type="button"
                    onClick={handleCancelForm}
                    className="btn-secondary py-2"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-primary py-2"
                  >
                    Save Address
                  </button>
                </div>

              </form>
            )}

            {/* Address list */}
            {addressesLoading ? (
              <div className="animate-pulse h-24 bg-surface-100 rounded-xl"></div>
            ) : isAdmin ? (
              <div className="text-center py-8 bg-surface-50 rounded-xl border border-dashed border-red-200">
                 <p className="text-red-500 text-sm font-bold">Admin accounts cannot save shipping addresses.</p>
              </div>
            ) : addresses.length === 0 && !showForm ? (
              <div className="text-center py-8 bg-surface-50 rounded-xl border border-dashed border-surface-300">
                 <p className="text-surface-500 text-sm font-medium">No shipping addresses saved yet.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {addresses.map((addr) => (
                  <div key={addr.id} className="bg-white border border-surface-200 hover:border-brand-300 transition-colors rounded-xl p-5 flex flex-col justify-between h-40 shadow-sm relative">
                    <div>
                      <div className="flex justify-between items-center mb-1">
                        <span className="font-bold text-base text-surface-900">{addr.title}</span>
                        {addr.isDefault && <span className="bg-surface-100 text-surface-600 px-2 py-0.5 rounded text-[10px] font-bold tracking-widest">DEFAULT</span>}
                      </div>
                      <p className="text-surface-600 mt-2 text-sm line-clamp-1">{addr.streetAddress}</p>
                      <p className="text-surface-600 text-sm">{addr.city}, {addr.state} {addr.zipCode}</p>
                    </div>
                    <div className="flex justify-end space-x-2 pt-3 border-t border-surface-100 mt-2">
                      <button
                        onClick={() => handleEditAddress(addr)}
                        className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-brand-600 hover:border-brand-200 transition-colors shadow-sm"
                      >
                        <Edit2 className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteAddress(addr.id)}
                        className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors shadow-sm"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

          </div>

          {/* Recent Orders Tracker */}
          <div className="card p-6 rounded-2xl shadow-sm mt-6">
            <div className="flex justify-between items-center border-b border-surface-100 pb-4 mb-4">
              <div>
                <h3 className="font-bold text-xl text-surface-900">Recent Orders</h3>
                <p className="text-xs font-medium text-surface-500 mt-0.5">Track your latest purchases</p>
              </div>
              <Link to="/orders" className="text-brand-600 hover:text-brand-700 text-sm font-bold flex items-center group">
                View All <ChevronRight className="h-4 w-4 ml-0.5 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </div>

            {ordersLoading ? (
               <div className="animate-pulse h-20 bg-surface-100 rounded-xl"></div>
            ) : recentOrders.length === 0 ? (
               <div className="text-center py-6 bg-surface-50 rounded-xl border border-dashed border-surface-300">
                 <Package className="h-8 w-8 text-surface-300 mx-auto mb-2" />
                 <p className="text-surface-500 text-sm font-medium">No recent orders found.</p>
               </div>
            ) : (
               <div className="space-y-3">
                 {recentOrders.map(order => (
                   <div key={order.id} className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-4 bg-white border border-surface-200 rounded-xl hover:border-brand-200 transition-colors">
                     <div className="flex items-center space-x-3 mb-2 sm:mb-0">
                       <div className="h-10 w-10 bg-brand-50 text-brand-600 rounded-lg flex items-center justify-center shrink-0 border border-brand-100">
                         <Package className="h-5 w-5" />
                       </div>
                       <div>
                         <span className="font-bold text-sm text-surface-900 block">{order.orderNumber}</span>
                         <span className="text-xs text-surface-500">{new Date(order.createdAt).toLocaleDateString()} • {order.items?.length || 0} items</span>
                       </div>
                     </div>
                     <div className="flex items-center w-full sm:w-auto justify-between sm:justify-end gap-4 ml-13 sm:ml-0">
                       <span className="font-bold text-surface-900">${order.finalAmount.toFixed(2)}</span>
                       <span className={`text-[10px] font-extrabold uppercase px-2.5 py-1 rounded tracking-wider ${
                         ['PENDING','PROCESSING'].includes(order.status) ? 'bg-amber-100 text-amber-700' :
                         ['SHIPPED','DELIVERED'].includes(order.status) ? 'bg-emerald-100 text-emerald-700' :
                         'bg-surface-100 text-surface-600'
                       }`}>
                         {order.status}
                       </span>
                     </div>
                   </div>
                 ))}
               </div>
            )}
          </div>

        </div>

      </div>

    </div>
  )
}

export default ProfilePage
