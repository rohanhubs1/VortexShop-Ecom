import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { User, Mail, Lock, Phone, AlertCircle, Shield } from 'lucide-react'

const Register = () => {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)
  
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(firstName, lastName, email, password, phone)
      setSuccess(true)
      setTimeout(() => {
        navigate('/login')
      }, 2000)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-12 bg-surface-50">
      <div className="max-w-md w-full card p-8 relative overflow-hidden">
        
        {/* Subtle decorative background elements */}
        <div className="absolute -top-24 -right-24 w-48 h-48 bg-brand-100 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
        <div className="absolute -bottom-24 -left-24 w-48 h-48 bg-emerald-100 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

        <div className="text-center mb-8 relative z-10">
          <div className="mx-auto h-16 w-16 bg-brand-50 rounded-full flex items-center justify-center text-brand-600 mb-4 shadow-sm border border-brand-100">
            <Shield className="h-8 w-8" />
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight text-surface-900">Create Account</h2>
          <p className="text-sm text-surface-500 mt-2 font-medium">Get started with a customer account today</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start space-x-3 text-sm font-medium z-10 relative">
            <AlertCircle className="h-5 w-5 shrink-0 mt-0.5 text-red-500" />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="mb-6 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg text-sm text-center font-bold z-10 relative">
            Registration successful! Redirecting to login...
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5 relative z-10">
          
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-2">First Name</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-surface-400">
                  <User className="h-4 w-4" />
                </span>
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="Alex"
                  className="input-field py-2.5 pl-9 text-sm"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-2">Last Name</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-surface-400">
                  <User className="h-4 w-4" />
                </span>
                <input
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder="Dev"
                  className="input-field py-2.5 pl-9 text-sm"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-2">Email Address</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-surface-400">
                <Mail className="h-5 w-5" />
              </span>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="customer@vortex.com"
                className="input-field py-2.5 pl-11 text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-2">Mobile Phone</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-surface-400">
                <Phone className="h-5 w-5" />
              </span>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 555 0200"
                className="input-field py-2.5 pl-11 text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-2">Secure Password</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-surface-400">
                <Lock className="h-5 w-5" />
              </span>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Min 6 characters"
                className="input-field py-2.5 pl-11 text-sm"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full btn-primary py-3 text-base shadow-md mt-2"
          >
            {loading ? 'Registering...' : 'Register Account'}
          </button>
        </form>

        <div className="mt-8 text-center text-sm text-surface-600 relative z-10 font-medium">
          <span>Already have an account? </span>
          <Link to="/login" className="text-brand-600 hover:text-brand-700 hover:underline font-bold">Login here</Link>
        </div>

      </div>
    </div>
  )
}

export default Register
