import React, { useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import API from '../api/axios'
import toast from 'react-hot-toast'
import { Lock } from 'lucide-react'

const ResetPassword = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()
  
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!token) {
      toast.error('Invalid token.')
      return
    }
    if (password !== confirmPassword) {
      toast.error('Passwords do not match.')
      return
    }
    
    setLoading(true)
    try {
      await API.post('/auth/reset-password', { token, newPassword: password })
      toast.success('Password reset successfully! You can now log in.')
      navigate('/login')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reset password.')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface-50">
        <h2 className="text-xl font-bold text-red-500">Invalid or missing reset token.</h2>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-surface-50">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-surface-200">
        <div className="text-center">
          <Lock className="mx-auto h-12 w-12 text-brand-600" />
          <h2 className="mt-6 text-3xl font-extrabold text-surface-900 tracking-tight">Set New Password</h2>
        </div>
        
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-bold text-surface-700 mb-1">New Password</label>
              <input
                type="password"
                required
                className="input-field"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-surface-700 mb-1">Confirm New Password</label>
              <input
                type="password"
                required
                className="input-field"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
          </div>
          
          <button type="submit" disabled={loading} className="w-full btn-primary py-3">
            {loading ? 'Resetting...' : 'Reset Password'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default ResetPassword
