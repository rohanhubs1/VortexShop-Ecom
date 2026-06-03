import React, { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { CheckCircle, ShoppingBag, ArrowRight } from 'lucide-react'
import confetti from 'canvas-confetti'

const OrderSuccessPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  
  useEffect(() => {
    // If user arrived here without placing an order, redirect home
    if (!location.state?.orderSuccess) {
      navigate('/')
      return
    }

    // Trigger confetti!
    const end = Date.now() + 2 * 1000
    const colors = ['#0d9488', '#34D399'] // Teal/Emerald colors
    
    ;(function frame() {
      confetti({
        particleCount: 3,
        angle: 60,
        spread: 55,
        origin: { x: 0 },
        colors: colors
      })
      confetti({
        particleCount: 3,
        angle: 120,
        spread: 55,
        origin: { x: 1 },
        colors: colors
      })
      if (Date.now() < end) {
        requestAnimationFrame(frame)
      }
    })()
  }, [location, navigate])

  if (!location.state?.orderSuccess) return null

  return (
    <div className="min-h-[70vh] flex items-center justify-center py-12">
      <div className="card p-10 rounded-3xl max-w-lg w-full text-center space-y-6 shadow-xl relative overflow-hidden border-t-4 border-t-emerald-500">
        
        {/* Background glow */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-emerald-100 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

        <div className="flex justify-center relative">
          <div className="h-24 w-24 bg-emerald-50 border-4 border-emerald-100 rounded-full flex items-center justify-center shadow-inner">
            <CheckCircle className="h-12 w-12 text-emerald-500" />
          </div>
        </div>

        <div className="space-y-2 relative">
          <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">Order Confirmed!</h1>
          <p className="text-surface-600 text-sm max-w-sm mx-auto font-medium">
            Thank you for your purchase. We have received your order and are currently processing it.
          </p>
        </div>

        <div className="bg-surface-50 border border-surface-200 rounded-2xl p-6 text-left space-y-3 relative shadow-sm">
          <div className="flex justify-between items-center text-sm">
            <span className="text-surface-500 font-bold">Order Date:</span>
            <span className="text-surface-900 font-bold">{new Date().toLocaleDateString()}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-surface-500 font-bold">Status:</span>
            <span className="bg-amber-100 text-amber-800 border border-amber-200 px-2 py-1 rounded text-[10px] font-bold tracking-wider">PROCESSING</span>
          </div>
          <p className="text-xs text-surface-500 text-center pt-3 mt-1 border-t border-surface-200 font-medium">
            You will receive an email confirmation shortly with your order details.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 pt-4 relative">
          <Link
            to="/orders"
            className="flex-1 btn-secondary py-3 text-sm flex items-center justify-center space-x-2"
          >
            <ShoppingBag className="h-4 w-4" />
            <span>Track Order</span>
          </Link>
          <Link
            to="/catalog"
            className="flex-1 btn-primary py-3 text-sm flex items-center justify-center space-x-2"
          >
            <span>Continue Shopping</span>
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>

      </div>
    </div>
  )
}

export default OrderSuccessPage
