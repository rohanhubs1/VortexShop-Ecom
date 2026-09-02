import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Shield, Mail, Lock, AlertCircle, Eye, EyeOff } from 'lucide-react'

const Login = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const from = location.state?.from?.pathname || '/'

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await login(email, password)
      if (user.roles.includes('ROLE_ADMIN')) {
        navigate('/admin/dashboard')
      } else {
        navigate(from, { replace: true })
      }
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-surface-50">
      <div className="max-w-md w-full card p-8 relative overflow-hidden">
        
        {/* Subtle decorative background elements */}
        <div className="absolute -top-24 -right-24 w-48 h-48 bg-brand-100 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
        <div className="absolute -bottom-24 -left-24 w-48 h-48 bg-emerald-100 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

        <div className="text-center mb-8 relative z-10">
          <div className="mx-auto h-16 w-16 bg-brand-50 rounded-full flex items-center justify-center text-brand-600 mb-4 shadow-sm border border-brand-100">
            <Shield className="h-8 w-8" />
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight text-surface-900">Welcome Back</h2>
          <p className="text-sm text-surface-500 mt-2 font-medium">Sign in to access your account</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start space-x-3 text-sm font-medium z-10 relative">
            <AlertCircle className="h-5 w-5 shrink-0 mt-0.5 text-red-500" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6 relative z-10">
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
                className="input-field py-3 pl-11 text-sm"
              />
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center mb-2">
              <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Password</label>
              <Link to="/forgot-password" className="text-xs font-semibold text-brand-600 hover:text-brand-500">Forgot password?</Link>
            </div>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-surface-400">
                <Lock className="h-5 w-5" />
              </span>
              <input
                type={showPassword ? "text" : "password"}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="input-field py-3 pl-11 pr-11 text-sm"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-surface-400 hover:text-brand-600 focus:outline-none"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full btn-primary py-3 text-base shadow-md"
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className="mt-8 text-center text-sm text-surface-600 relative z-10 font-medium">
          <span>New to VortexShop? </span>
          <Link to="/register" className="text-brand-600 hover:text-brand-700 hover:underline font-bold">Create an account</Link>
        </div>

      </div>
    </div>
  )
}

export default Login
