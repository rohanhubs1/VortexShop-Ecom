import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import API from '../api/axios'
import { ShoppingBag, ArrowRight, Star, Heart, Flame, Sparkles, TrendingUp, Tag, Award } from 'lucide-react'
import { useCart } from '../context/CartContext'
import Carousel from '../components/Carousel'
import { getImageUrl } from '../utils/imageHelper'

const Home = () => {
  const [products, setProducts] = useState([])
  const [topDeals, setTopDeals] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const { addToCart, addToWishlist, isInWishlist, removeFromWishlist } = useCart()

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prodRes, catRes] = await Promise.all([
          API.get('/products?size=8&sort=createdAt,desc'),
          API.get('/categories')
        ])
        
        const allProducts = prodRes.data.content || []
        
        // Split into sections (repeat data if not enough to show infinite loop scrolling effect)
        const extendItems = (items, min) => items.length < min ? [...items, ...items, ...items].slice(0, min) : items;
        
        setTopDeals(extendItems(allProducts.slice(0, 4), 6))
        setProducts(extendItems(allProducts.slice(4, 8), 6))
        // We'll just reuse some products for best sellers for demo purposes
        setCategories(catRes.data)
      } catch (err) {
        console.error('Failed to load home page content', err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  const handleWishlist = async (productId) => {
    try {
      if (isInWishlist(productId)) {
        await removeFromWishlist(productId)
        toast.success("Removed from wishlist")
      } else {
        await addToWishlist(productId)
        toast.success("Added to wishlist")
      }
    } catch (err) {
      toast.error(err)
    }
  }

  const handleAddToCart = async (prodId) => {
    try {
      await addToCart(prodId)
      toast.success("Added to cart!")
    } catch (err) {
      toast.error(err)
    }
  }

  const ProductCard = ({ prod, badge }) => (
    <div className="w-full h-full card flex flex-col relative group">
      
      {/* Badges */}
      {badge && (
        <div className="absolute top-3 left-3 z-10 bg-brand-500 text-white text-[10px] font-bold px-2 py-1 rounded shadow-sm flex items-center gap-1 uppercase tracking-wider">
          {badge === 'HOT' ? <Flame className="h-3 w-3" /> : <Tag className="h-3 w-3" />}
          <span>{badge}</span>
        </div>
      )}

      {/* Favorite Toggle */}
      <button
        onClick={() => handleWishlist(prod.id)}
        className={`absolute top-3 right-3 p-2 rounded-full border transition-all z-10 bg-white/80 backdrop-blur-sm shadow-sm hover:scale-105 ${
          isInWishlist(prod.id)
            ? 'border-red-100 text-red-500'
            : 'border-transparent text-surface-400 hover:text-brand-500 hover:border-brand-100'
        }`}
      >
        <Heart className={`h-4 w-4 ${isInWishlist(prod.id) ? 'fill-current' : ''}`} />
      </button>

      {/* Product Image Wrapper */}
      <div className="relative bg-surface-50 h-56 flex items-center justify-center p-6 rounded-t-xl overflow-hidden border-b border-surface-100 group-hover:bg-surface-100 transition-colors duration-300">
        <img 
          src={getImageUrl(prod.imageUrl)} 
          alt={prod.name} 
          className="object-contain h-full w-full group-hover:scale-105 transition-transform duration-500 mix-blend-multiply"
        />
      </div>

      {/* Details */}
      <div className="p-4 flex-1 flex flex-col justify-between">
        <div className="space-y-1">
          <div className="flex justify-between items-center">
            <span className="text-[10px] text-brand-600 uppercase tracking-widest font-bold">{prod.categoryName}</span>
            <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${prod.stockQuantity > 5 ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'}`}>
              {prod.stockQuantity > 5 ? 'IN STOCK' : `ONLY ${prod.stockQuantity} LEFT`}
            </span>
          </div>
          <Link to={`/products/${prod.id}`} className="block font-bold text-base text-surface-900 hover:text-brand-600 transition-colors line-clamp-2 leading-tight">
            {prod.name}
          </Link>
          
          <div className="flex items-center space-x-1.5 pt-1">
            <div className="flex text-amber-400">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className={`h-3.5 w-3.5 ${i < Math.floor(prod.averageRating || 4.8) ? 'fill-current' : 'text-surface-200'}`} />
              ))}
            </div>
            <span className="text-xs text-surface-500 font-medium">({prod.averageRating || '4.8'})</span>
          </div>
        </div>

        <div className="flex items-end justify-between mt-4 pt-4 border-t border-surface-100">
          <div className="flex flex-col">
            {badge === 'HOT' && <span className="text-xs text-surface-400 line-through">${(prod.price * 1.2).toFixed(2)}</span>}
            <span className="text-lg font-extrabold text-surface-900">${prod.price}</span>
          </div>
          <button
            onClick={() => handleAddToCart(prod.id)}
            disabled={prod.stockQuantity < 1}
            className="btn-primary py-1.5 px-4 text-xs shadow-sm hover:shadow-md"
          >
            <span>{prod.stockQuantity > 0 ? 'Add to Cart' : 'Out of Stock'}</span>
          </button>
        </div>
      </div>
    </div>
  )

  return (
    <div className="space-y-16 pb-16 overflow-hidden">
      
      {/* Hero Banner */}
      <section className="relative rounded-2xl overflow-hidden bg-surface-900 py-20 px-6 md:px-16 shadow-xl text-center md:text-left flex flex-col md:flex-row items-center justify-between mt-6">
        <div className="absolute top-0 right-0 w-full h-full opacity-40 mix-blend-overlay">
          <img src="https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=1600" alt="Hero background" className="object-cover w-full h-full" />
        </div>
        <div className="absolute inset-0 bg-gradient-to-r from-surface-900 via-surface-900/90 to-transparent"></div>

        <div className="max-w-2xl space-y-6 relative z-10">
          <div className="inline-flex items-center space-x-2 bg-brand-500/20 text-brand-300 px-3 py-1.5 rounded-full text-xs font-semibold uppercase tracking-wider">
            <Sparkles className="h-3.5 w-3.5" />
            <span>Premium E-Commerce Experience</span>
          </div>
          <h1 className="text-5xl md:text-6xl font-extrabold leading-tight text-white tracking-tight">
            Discover Exceptional <br/>
            <span className="text-brand-400">Products Everyday</span>
          </h1>
          <p className="text-surface-300 text-lg max-w-lg mx-auto md:mx-0 leading-relaxed font-light">
            Shop the latest trends in electronics, fashion, and home goods with curated collections delivered to your door.
          </p>
          <div className="pt-4 flex flex-col sm:flex-row justify-center md:justify-start space-y-3 sm:space-y-0 sm:space-x-4">
            <Link to="/catalog" className="bg-brand-500 hover:bg-brand-400 text-white font-bold px-8 py-3.5 rounded-lg flex items-center justify-center space-x-2 transition-all shadow-lg hover:shadow-brand-500/30">
              <span>Shop Collection</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>

        <div className="hidden lg:block relative z-10 w-80">
          <div className="bg-white rounded-2xl p-6 shadow-2xl transform rotate-3 hover:rotate-0 transition-transform duration-300 border border-surface-100">
            <img src="https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500" alt="Featured Product" className="w-full h-48 object-cover rounded-xl mb-4" />
            <h3 className="font-bold text-surface-900 text-lg">Sony WH-1000XM5</h3>
            <p className="text-sm text-surface-500 mb-3">Industry leading noise cancellation</p>
            <div className="flex justify-between items-center">
              <span className="font-extrabold text-brand-600 text-xl">$348.00</span>
              <span className="bg-surface-100 text-surface-600 text-xs px-2 py-1 rounded font-medium">FEATURED</span>
            </div>
          </div>
        </div>
      </section>

      {/* Top Deals Carousel */}
      <section className="space-y-6">
        <div className="flex justify-between items-end border-b border-surface-200 pb-2">
          <div>
            <h2 className="text-2xl font-bold tracking-tight text-surface-900">Top Deals Today</h2>
          </div>
          <Link to="/catalog" className="text-brand-600 hover:text-brand-700 text-sm font-semibold flex items-center space-x-1.5 transition-colors">
            <span>View All</span>
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>

        {loading ? (
          <div className="flex gap-6 overflow-hidden">
            {Array(4).fill(0).map((_, idx) => (
              <div key={idx} className="shrink-0 w-[280px] h-96 bg-surface-100 rounded-xl animate-pulse"></div>
            ))}
          </div>
        ) : (
          <Carousel 
            items={topDeals} 
            renderItem={(prod) => <ProductCard prod={prod} badge="HOT" />} 
            slideInterval={4500} 
          />
        )}
      </section>

      {/* Categories Grid */}
      <section className="space-y-6">
        <div className="flex justify-between items-end">
          <div>
            <h2 className="text-2xl font-bold tracking-tight text-surface-900">Shop by Category</h2>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {loading ? (
            Array(6).fill(0).map((_, idx) => (
              <div key={idx} className="h-32 bg-surface-100 rounded-xl animate-pulse"></div>
            ))
          ) : (
            categories.slice(0, 6).map((cat) => (
              <Link
                key={cat.id}
                to={`/catalog?category=${cat.id}`}
                className="group card flex flex-col items-center justify-center p-6 text-center hover:border-brand-500/50"
              >
                <div className="bg-surface-50 text-brand-600 p-4 rounded-full mb-3 group-hover:bg-brand-50 transition-colors">
                  <ShoppingBag className="h-6 w-6" />
                </div>
                <h3 className="font-semibold text-surface-900 text-sm group-hover:text-brand-600 transition-colors">{cat.name}</h3>
              </Link>
            ))
          )}
        </div>
      </section>

      {/* New Arrivals */}
      <section className="space-y-6">
        <div className="flex justify-between items-end border-b border-surface-200 pb-2">
          <div>
            <h2 className="text-2xl font-bold tracking-tight text-surface-900">New Arrivals</h2>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {loading ? (
            Array(4).fill(0).map((_, idx) => (
              <div key={idx} className="h-96 bg-surface-100 rounded-xl animate-pulse"></div>
            ))
          ) : (
            products.map((prod) => <ProductCard key={prod.id} prod={prod} badge="NEW" />)
          )}
        </div>
      </section>

    </div>
  )
}

export default Home
