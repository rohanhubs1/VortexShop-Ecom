import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import API from '../api/axios'
import toast from 'react-hot-toast'
import { KeyRound, ArrowLeft } from 'lucide-react'

const ForgotPassword = () => {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!email) return
    setLoading(true)
    try {
      await API.post('/auth/forgot-password', { email })
      setSent(true)
      toast.success('Reset link sent to your email!')
    } catch (err) {
      toast.error('Failed to send reset link.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-surface-50">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-surface-200">
        <div className="text-center">
          <KeyRound className="mx-auto h-12 w-12 text-brand-600" />
          <h2 className="mt-6 text-3xl font-extrabold text-surface-900 tracking-tight">Forgot Password</h2>
          <p className="mt-2 text-sm text-surface-600">
            Enter your email to receive a password reset link.
          </p>
        </div>
        
        {sent ? (
          <div className="text-center animate-fade-in bg-emerald-50 p-6 rounded-xl border border-emerald-100">
            <p className="text-emerald-700 font-semibold mb-6">Check your email for the reset link.</p>
            <Link to="/login" className="btn-primary w-full py-2.5 flex items-center justify-center gap-2">
              <ArrowLeft className="h-4 w-4" /> Back to Login
            </Link>
          </div>
        ) : (
          <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-sm font-bold text-surface-700 mb-1">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                required
                className="input-field"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            
            <div>
              <button type="submit" disabled={loading} className="w-full btn-primary py-3">
                {loading ? 'Sending...' : 'Send Reset Link'}
              </button>
            </div>
            
            <div className="text-center">
              <Link to="/login" className="text-sm font-semibold text-brand-600 hover:text-brand-500">
                Wait, I remember my password
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default ForgotPassword
