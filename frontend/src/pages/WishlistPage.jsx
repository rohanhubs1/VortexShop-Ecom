import React from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { getImageUrl } from '../utils/imageHelper'
import { Trash2, ShoppingCart, Heart, Star } from 'lucide-react'

const WishlistPage = () => {
  const { wishlist, wishlistLoading, removeFromWishlist, addToCart } = useCart()

  if (wishlistLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  const isEmpty = !wishlist || !wishlist.products || wishlist.products.length === 0

  return (
    <div className="py-8 space-y-8 max-w-7xl mx-auto">
      <div className="border-b border-surface-200 pb-6">
        <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">My Wishlist</h1>
        <p className="text-surface-500 text-sm mt-1">Keep track of items you want to purchase</p>
      </div>

      {isEmpty ? (
        <div className="card rounded-2xl p-16 text-center space-y-4 max-w-2xl mx-auto border-dashed border-2 border-surface-200 bg-surface-50">
          <Heart className="h-16 w-16 text-surface-300 mx-auto fill-current" />
          <h2 className="text-2xl font-bold text-surface-900">Your Wishlist is Empty</h2>
          <p className="text-surface-500 text-sm">Save items to your wishlist by clicking the heart icons during browsing.</p>
          <Link to="/catalog" className="btn-primary inline-block px-8">
            Start Browsing
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {wishlist.products.map((prod) => (
            <div key={prod.id} className="card rounded-2xl overflow-hidden group flex flex-col justify-between h-full hover:border-brand-300 transition-colors">
              
              <div className="relative bg-surface-50 h-56 flex items-center justify-center p-6 rounded-t-xl overflow-hidden border-b border-surface-100 group-hover:bg-surface-100 transition-colors duration-300">
                <img 
                  src={getImageUrl(prod.imageUrl)} 
                  alt={prod.name} 
                  className="object-contain h-full w-full group-hover:scale-105 transition-transform duration-500 mix-blend-multiply" 
                />
                <button
                  onClick={() => removeFromWishlist(prod.id)}
                  className="absolute top-4 right-4 p-2 rounded-full bg-white border border-surface-200 text-surface-400 hover:bg-red-50 hover:text-red-500 hover:border-red-200 transition-colors shadow-sm"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>

              <div className="p-5 flex flex-col flex-1">
                <div className="space-y-1 flex-1">
                  <span className="text-[10px] text-brand-600 uppercase tracking-widest font-extrabold">{prod.categoryName}</span>
                  <Link to={`/products/${prod.id}`} className="block font-bold text-base text-surface-900 hover:text-brand-600 transition-colors line-clamp-2 leading-tight">
                    {prod.name}
                  </Link>
                  <div className="flex items-center space-x-1 pt-1.5">
                    <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
                    <span className="text-sm text-surface-600 font-semibold">{prod.averageRating || '5.0'}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-4 mt-4 border-t border-surface-100">
                  <span className="text-lg font-extrabold text-surface-900">${prod.price}</span>
                  <button
                    onClick={() => {
                      addToCart(prod.id, 1)
                      removeFromWishlist(prod.id)
                    }}
                    className="btn-primary py-1.5 px-3.5 text-xs flex items-center space-x-1.5 shadow-sm"
                  >
                    <ShoppingCart className="h-3.5 w-3.5" />
                    <span>Move to Cart</span>
                  </button>
                </div>
              </div>

            </div>
          ))}
        </div>
      )}

    </div>
  )
}

export default WishlistPage
