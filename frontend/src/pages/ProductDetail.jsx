import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import API from '../api/axios'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { getImageUrl } from '../utils/imageHelper'
import { Star, Heart, MessageSquare, ShieldCheck, ArrowLeft, Plus, Minus, Flame, Upload } from 'lucide-react'
import toast from 'react-hot-toast'

const ProductDetail = () => {
  const { id } = useParams()
  const { isAuthenticated, isAdmin } = useAuth()
  const { addToCart, addToWishlist, isInWishlist, removeFromWishlist } = useCart()

  // State
  const [product, setProduct] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [quantity, setQuantity] = useState(1)
  
  // Review form state
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [reviewError, setReviewError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState(false)
  const [reviewImage, setReviewImage] = useState(null)
  const [uploadingImage, setUploadingImage] = useState(false)

  // Recently Viewed State
  const [recentlyViewed, setRecentlyViewed] = useState([])

  const loadData = async () => {
    setLoading(true)
    try {
      const [prodRes, revRes] = await Promise.all([
        API.get(`/products/${id}`),
        API.get(`/products/${id}/reviews`)
      ])
      setProduct(prodRes.data)
      setReviews(revRes.data)
      
      // Update Recently Viewed in LocalStorage
      updateRecentlyViewed(prodRes.data)
    } catch (err) {
      console.error('Failed to load product details', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
    // Load other recently viewed from localstorage
    const local = JSON.parse(localStorage.getItem('recentlyViewed') || '[]')
    setRecentlyViewed(local.filter(p => p.id !== parseInt(id)))
  }, [id])

  const updateRecentlyViewed = (prod) => {
    let list = JSON.parse(localStorage.getItem('recentlyViewed') || '[]')
    // Filter out current product to prevent duplicate and append to front
    list = list.filter(item => item.id !== prod.id)
    list.unshift({ id: prod.id, name: prod.name, price: prod.price, categoryId: prod.categoryId, categoryName: prod.categoryName, imageUrl: prod.imageUrl })
    // Cap at 4 items
    list = list.slice(0, 4)
    localStorage.setItem('recentlyViewed', JSON.stringify(list))
  }

  const handleReviewSubmit = async (e) => {
    e.preventDefault()
    setReviewError('')
    setReviewSuccess(false)
    setUploadingImage(true)
    try {
      let imageUrl = null
      if (reviewImage) {
        const formData = new FormData()
        formData.append('file', reviewImage)
        const uploadRes = await API.post('/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        imageUrl = uploadRes.data.url
      }
      
      await API.post('/products/reviews', { productId: id, rating, comment, imageUrl })
      setReviewSuccess(true)
      setComment('')
      setReviewImage(null)
      // Re-fetch data
      const [prodRes, revRes] = await Promise.all([
        API.get(`/products/${id}`),
        API.get(`/products/${id}/reviews`)
      ])
      setProduct(prodRes.data)
      setReviews(revRes.data)
    } catch (err) {
      setReviewError(err.response?.data?.message || 'Failed to submit review')
    } finally {
      setUploadingImage(false)
    }
  }

  const handleAddToCart = async () => {
    try {
      await addToCart(product.id, quantity)
      toast.success("Added to cart!")
    } catch (err) {
      toast.error(err)
    }
  }

  const handleWishlist = async () => {
    try {
      if (isInWishlist(product.id)) {
        await removeFromWishlist(product.id)
        toast.success("Removed from wishlist")
      } else {
        await addToWishlist(product.id)
        toast.success("Added to wishlist")
      }
    } catch (err) {
      toast.error(err)
    }
  }

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-500"></div>
      </div>
    )
  }

  if (!product) {
    return (
      <div className="py-16 text-center">
        <h2 className="text-xl font-bold text-surface-900">Product not found</h2>
        <Link to="/catalog" className="text-brand-600 hover:underline mt-4 inline-block">Return to Catalog</Link>
      </div>
    )
  }

  return (
    <div className="py-8 space-y-12">
      
      {/* Back Button */}
      <Link to="/catalog" className="inline-flex items-center space-x-2 text-sm text-surface-500 hover:text-brand-600 transition-colors">
        <ArrowLeft className="h-4 w-4" />
        <span>Back to Catalog</span>
      </Link>

      {/* Main Grid Info */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-start">
        
        {/* Left Column: Product Image */}
        <div className="card p-12 aspect-[4/3] flex items-center justify-center relative overflow-hidden bg-white">
          <img 
            src={getImageUrl(product.imageUrl)} 
            alt={product.name} 
            className="object-contain h-full w-full hover:scale-105 transition-transform duration-500 mix-blend-multiply"
          />
        </div>

        {/* Right Column: Actions */}
        <div className="space-y-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs font-black px-3 py-1 bg-brand-100 text-brand-700 rounded-full uppercase tracking-widest">
                {product.categoryName}
              </span>
              {product.isTopDeal && (
                <span className="text-xs font-black px-3 py-1 bg-brand-500 text-white rounded-full uppercase tracking-widest flex items-center gap-1">
                  <Flame className="h-3 w-3" /> TOP DEAL
                </span>
              )}
              {product.averageRating > 4.7 && (
                <span className="text-xs font-black px-3 py-1 bg-emerald-500 text-white rounded-full uppercase tracking-widest flex items-center gap-1">
                  <Star className="h-3 w-3 fill-current" /> TOP RATED
                </span>
              )}
            </div>
            <h1 className="text-4xl md:text-5xl font-extrabold text-surface-300 tracking-tight">{product.name}</h1>
          </div>

          <div className="flex items-center space-x-4 border-b border-surface-200 pb-6">
            <div className="flex text-amber-400">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className={`h-4 w-4 ${i < Math.floor(product.averageRating || 0) ? 'fill-current' : 'text-surface-600'}`} />
              ))}
            </div>
            <span className="font-bold text-surface-300 text-sm">{product.averageRating || 'No ratings'}</span>
            <span className="text-surface-500">|</span>
            <span className="text-sm text-surface-500 flex items-center gap-2"><MessageSquare className="h-4 w-4" /> {reviews.length} Customer Reviews</span>
          </div>

          <p className="text-surface-600 text-base leading-relaxed">{product.description}</p>

          <div className="space-y-2">
            <span className="text-xs font-semibold text-surface-500 uppercase tracking-wider block">Pricing</span>
            <div className="flex items-end gap-3">
              <span className="text-4xl font-black text-surface-300">
                ${product.isTopDeal && product.discountPercentage > 0 
                  ? (product.price * (1 - product.discountPercentage / 100)).toFixed(2) 
                  : product.price}
              </span>
              {product.isTopDeal && product.discountPercentage > 0 && (
                <span className="text-lg font-bold text-surface-500 line-through mb-1">
                  ${product.price.toFixed(2)}
                </span>
              )}
            </div>
          </div>

          {/* Stock & Cart actions */}
          <div className="p-6 bg-surface-50 rounded-2xl border border-surface-200 space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-surface-700">Inventory Status:</span>
              {product.stockQuantity > 0 ? (
                <span className="text-xs font-semibold bg-emerald-100 text-emerald-700 px-3 py-1 rounded-full">
                  In Stock ({product.stockQuantity} remaining)
                </span>
              ) : (
                <span className="text-xs font-semibold bg-red-100 text-red-700 px-3 py-1 rounded-full">
                  Out of Stock
                </span>
              )}
            </div>

            {product.stockQuantity > 0 && (
              <div className="flex items-center space-x-4">
                <span className="text-sm font-medium text-surface-700">Quantity:</span>
                <div className="flex items-center bg-white border border-surface-300 rounded-lg">
                  <button
                    disabled={quantity <= 1}
                    onClick={() => setQuantity(quantity - 1)}
                    className="p-2 text-surface-500 hover:text-surface-900 disabled:opacity-30"
                  >
                    <Minus className="h-4 w-4" />
                  </button>
                  <span className="px-4 text-sm font-bold text-surface-900">{quantity}</span>
                  <button
                    disabled={quantity >= product.stockQuantity}
                    onClick={() => setQuantity(quantity + 1)}
                    className="p-2 text-surface-500 hover:text-surface-900 disabled:opacity-30"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}

            <div className="flex gap-4 pt-2">
              <button
                disabled={product.stockQuantity <= 0}
                onClick={handleAddToCart}
                className="flex-1 btn-primary py-3 rounded-xl text-sm flex items-center justify-center space-x-2"
              >
                <span>Add {quantity} to Cart</span>
              </button>
              
              <button
                onClick={handleWishlist}
                className={`p-3 rounded-xl border transition-all ${
                  isInWishlist(product.id)
                    ? 'bg-red-50 border-red-200 text-red-500'
                    : 'bg-white border-surface-300 text-surface-500 hover:text-brand-600 hover:border-brand-200'
                }`}
              >
                <Heart className={`h-5 w-5 ${isInWishlist(product.id) ? 'fill-current' : ''}`} />
              </button>
            </div>
          </div>

          <div className="flex items-center space-x-3 text-sm text-surface-600 bg-surface-50 p-4 rounded-xl border border-surface-200">
            <ShieldCheck className="h-5 w-5 text-brand-600 shrink-0" />
            <span>Eligible for secure checkout and free expedited shipping.</span>
          </div>

        </div>
      </div>

      {/* Reviews Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12 pt-12 border-t border-surface-200">
        
        {/* Left Column: Create Review Form */}
        <div className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-surface-300">Ratings & Reviews</h2>
            <p className="text-sm text-surface-500 mt-1">Share your experience with this product</p>
          </div>

          {isAuthenticated && !isAdmin ? (
            <form onSubmit={handleReviewSubmit} className="card p-6 border-none bg-surface-50 space-y-4">
              
              {reviewSuccess && (
                <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg text-sm font-medium">
                  Review submitted successfully!
                </div>
              )}

              {reviewError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm font-medium">
                  {reviewError}
                </div>
              )}

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-surface-600 uppercase tracking-wider">Rating Score</label>
                <div className="flex space-x-1">
                  {[1, 2, 3, 4, 5].map((val) => (
                    <button
                      key={val}
                      type="button"
                      onClick={() => setRating(val)}
                      className="p-1 hover:scale-110 transition-transform focus:outline-none"
                    >
                      <Star className={`h-7 w-7 ${val <= rating ? 'fill-amber-400 text-amber-400' : 'text-surface-300'}`} />
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-surface-600 uppercase tracking-wider">Comments</label>
                <textarea
                  required
                  rows="4"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Tell us what you liked or disliked. Solid build quality? Great performance?"
                  className="input-field text-sm"
                ></textarea>
              </div>

              <div>
                <label className="block text-xs font-semibold text-surface-600 uppercase tracking-wider mb-1.5">Add Photo (Optional)</label>
                <div className="flex items-center gap-4">
                  <label className="cursor-pointer bg-surface-100 hover:bg-surface-200 text-surface-700 px-4 py-2 rounded-xl text-xs font-bold flex items-center transition-colors">
                    <Upload className="h-4 w-4 mr-2" />
                    {reviewImage ? reviewImage.name : 'Choose Image'}
                    <input type="file" className="hidden" accept="image/*" onChange={e => setReviewImage(e.target.files[0])} />
                  </label>
                  {reviewImage && (
                    <button type="button" onClick={() => setReviewImage(null)} className="text-xs text-red-600 font-bold hover:underline">Remove</button>
                  )}
                </div>
              </div>

              <button
                type="submit"
                disabled={uploadingImage}
                className="w-full btn-primary py-2.5 rounded-xl text-sm"
              >
                {uploadingImage ? 'Submitting...' : 'Submit Review'}
              </button>
            </form>
          ) : isAdmin ? (
            <div className="card p-6 text-center space-y-3 bg-surface-50 border-none">
              <p className="text-sm font-bold text-surface-600">Admins cannot submit reviews.</p>
            </div>
          ) : (
            <div className="card p-6 text-center space-y-3 bg-surface-50 border-none">
              <p className="text-sm text-surface-600">Please log in to submit a rating or review.</p>
              <Link to="/login" className="btn-secondary inline-block py-2 px-6 rounded-full text-sm">
                Sign In
              </Link>
            </div>
          )}
        </div>

        {/* Right Column: Reviews List */}
        <div className="lg:col-span-2 space-y-6">
          <h3 className="font-bold text-xl text-surface-300">Recent Customer Critiques ({reviews.length})</h3>
          
          {reviews.length === 0 ? (
            <p className="text-surface-500 text-sm italic bg-surface-50 p-6 rounded-xl border border-surface-200">There are no reviews for this product yet. Be the first to review!</p>
          ) : (
            <div className="space-y-4">
              {reviews.map((rev) => (
                <div key={rev.id} className="bg-white border border-surface-200 rounded-xl p-6 space-y-3 shadow-sm">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-sm text-surface-300 block">{rev.reviewerName}</span>
                      <span className="text-xs text-surface-500">
                        {new Date(rev.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}
                      </span>
                    </div>
                    
                    <div className="flex items-center space-x-0.5">
                      {Array(rev.rating).fill(0).map((_, i) => (
                        <Star key={i} className="h-4 w-4 fill-amber-400 text-amber-400" />
                      ))}
                      {Array(5 - rev.rating).fill(0).map((_, i) => (
                        <Star key={i} className="h-4 w-4 text-surface-200" />
                      ))}
                    </div>
                  </div>
                    <p className="text-sm text-surface-600 leading-relaxed">{rev.comment}</p>
                    {rev.imageUrl && (
                      <div className="mt-3">
                        <img src={getImageUrl(rev.imageUrl)} alt="Review attachment" className="h-24 w-24 object-cover rounded-lg border border-surface-200 shadow-sm hover:scale-105 transition-transform cursor-pointer" onClick={() => window.open(getImageUrl(rev.imageUrl), '_blank')} />
                      </div>
                    )}
                  </div>
              ))}
            </div>
          )}
        </div>

      </div>

      {/* Recently Viewed Products */}
      {recentlyViewed.length > 0 && (
        <div className="pt-12 border-t border-surface-200 space-y-6">
          <h2 className="text-2xl font-bold text-surface-300">Recently Viewed Products</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {recentlyViewed.map((p) => (
              <Link key={p.id} to={`/products/${p.id}`} className="card rounded-2xl overflow-hidden p-5 flex flex-col justify-between h-48 hover:border-brand-300">
                <div className="h-20 flex justify-center mb-2">
                  <img src={getImageUrl(p.imageUrl)} alt={p.name} className="h-full object-contain mix-blend-multiply" />
                </div>
                <div>
                  <span className="font-bold text-sm text-surface-900 line-clamp-2 leading-tight">{p.name}</span>
                </div>
                <div className="flex justify-between items-center mt-3 pt-3 border-t border-surface-100">
                  <span className="font-extrabold text-brand-600">${p.price}</span>
                  <span className="text-xs text-surface-500 font-semibold hover:text-brand-600">View Detail &rarr;</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

    </div>
  )
}

export default ProductDetail
