import React, { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import API from '../api/axios'
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react'

const VerifyEmail = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [status, setStatus] = useState('loading') // loading, success, error

  useEffect(() => {
    if (!token) {
      setStatus('error')
      return
    }

    const verify = async () => {
      try {
        await API.post('/auth/verify', { token })
        setStatus('success')
      } catch (err) {
        setStatus('error')
      }
    }

    verify()
  }, [token])

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-surface-50">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl text-center border border-surface-200">
        {status === 'loading' && (
          <div className="flex flex-col items-center">
            <Loader2 className="h-16 w-16 text-brand-600 animate-spin mb-4" />
            <h2 className="text-2xl font-extrabold text-surface-900">Verifying Email...</h2>
            <p className="mt-2 text-surface-600">Please wait while we verify your account.</p>
          </div>
        )}

        {status === 'success' && (
          <div className="flex flex-col items-center animate-fade-in">
            <CheckCircle2 className="h-16 w-16 text-emerald-500 mb-4" />
            <h2 className="text-2xl font-extrabold text-surface-900">Email Verified!</h2>
            <p className="mt-2 text-surface-600">Your account is now fully active.</p>
            <Link to="/login" className="mt-8 btn-primary w-full py-3">
              Go to Login
            </Link>
          </div>
        )}

        {status === 'error' && (
          <div className="flex flex-col items-center animate-fade-in">
            <XCircle className="h-16 w-16 text-red-500 mb-4" />
            <h2 className="text-2xl font-extrabold text-surface-900">Verification Failed</h2>
            <p className="mt-2 text-surface-600">The link is invalid or has expired.</p>
            <Link to="/login" className="mt-8 text-surface-700 btn-outline w-full py-3">
              Back to Login
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}

export default VerifyEmail
