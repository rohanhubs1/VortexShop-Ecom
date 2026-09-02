import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import API from '../api/axios'
import { useCart } from '../context/CartContext'
import { getImageUrl } from '../utils/imageHelper'
import { Filter, Star, Heart, Search, ChevronLeft, ChevronRight, SlidersHorizontal, Tag, Flame } from 'lucide-react'

const Catalog = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const { addToCart, addToWishlist, isInWishlist, removeFromWishlist } = useCart()

  // State
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  
  // Filters state from URL query or defaults
  const [search, setSearch] = useState(searchParams.get('search') || '')
  const [categoryId, setCategoryId] = useState(searchParams.get('category') || '')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [minRating, setMinRating] = useState('')
  const [sort, setSort] = useState('createdAt,desc')
  const [page, setPage] = useState(0)

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await API.get('/categories')
        setCategories(res.data)
      } catch (err) {
        console.error('Failed to load categories', err)
      }
    }
    fetchCategories()
  }, [])

  // Sync Search state with Search Param change
  useEffect(() => {
    const searchVal = searchParams.get('search') || ''
    const catVal = searchParams.get('category') || ''
    setSearch(searchVal)
    setCategoryId(catVal)
    setPage(0)
  }, [searchParams])

  const fetchProducts = async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      if (search) params.append('keyword', search)
      if (categoryId) params.append('categoryId', categoryId)
      if (minPrice) params.append('minPrice', minPrice)
      if (maxPrice) params.append('maxPrice', maxPrice)
      if (minRating) params.append('minRating', minRating)
      params.append('sort', sort)
      params.append('page', page)
      params.append('size', '9')

      const res = await API.get(`/products?${params.toString()}`)
      setProducts(res.data.content)
      setTotalPages(res.data.totalPages)
      setTotalElements(res.data.totalElements)
    } catch (err) {
      console.error('Failed to load products', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [search, categoryId, minPrice, maxPrice, minRating, sort, page])

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
      await addToCart(prodId, 1)
      toast.success("Added to cart!")
    } catch (err) {
      toast.error(err)
    }
  }

  const handleClearFilters = () => {
    setSearch('')
    setCategoryId('')
    setMinPrice('')
    setMaxPrice('')
    setMinRating('')
    setSort('createdAt,desc')
    setPage(0)
    setSearchParams({})
  }

  return (
    <div className="py-8 space-y-6">
      
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-surface-200 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-surface-300 tracking-tight">Products Catalog</h1>
          <p className="text-surface-500 text-sm mt-1">Showing {products.length} of {totalElements} results</p>
        </div>
        
        {/* Sort & Search Controls */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative">
            <input
              type="text"
              placeholder="Search catalog..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-white border border-surface-300 rounded-lg py-2 pl-4 pr-10 text-sm text-surface-900 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 w-full sm:w-64 transition-all shadow-sm"
            />
            <Search className="absolute right-3 top-2.5 h-4 w-4 text-surface-400" />
          </div>

          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="bg-white border border-surface-300 text-surface-700 rounded-lg px-4 py-2 text-sm focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 shadow-sm"
          >
            <option value="createdAt,desc">Latest Arrivals</option>
            <option value="price,asc">Price: Low to High</option>
            <option value="price,desc">Price: High to Low</option>
            <option value="averageRating,desc">Highest Rated</option>
          </select>
        </div>
      </div>

      {/* Main Grid */}
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        
        {/* Left Filter Sidebar */}
        <aside className="w-full lg:w-64 shrink-0 bg-white border border-surface-200 rounded-2xl p-6 h-fit space-y-8 shadow-sm sticky top-24">
          <div className="flex items-center justify-between border-b border-surface-100 pb-4">
            <span className="font-bold text-sm text-surface-900 uppercase tracking-wider flex items-center space-x-2">
              <SlidersHorizontal className="h-4 w-4 text-brand-600" />
              <span>Filters</span>
            </span>
            <button onClick={handleClearFilters} className="text-xs font-semibold text-brand-600 hover:text-brand-700">Clear All</button>
          </div>

          {/* Categories */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-surface-500">Categories</h3>
            <div className="space-y-1">
              <button
                onClick={() => setCategoryId('')}
                className={`w-full text-left text-sm py-2 px-3 rounded-lg transition-colors ${
                  !categoryId ? 'bg-brand-50 text-brand-700 font-semibold' : 'text-surface-600 hover:bg-surface-50'
                }`}
              >
                All Categories
              </button>
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setCategoryId(cat.id.toString())}
                  className={`w-full text-left text-sm py-2 px-3 rounded-lg transition-colors ${
                    categoryId === cat.id.toString() ? 'bg-brand-50 text-brand-700 font-semibold' : 'text-surface-600 hover:bg-surface-50'
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </div>

          {/* Price Range */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-surface-500">Price Range ($)</h3>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="number"
                placeholder="Min"
                value={minPrice}
                onChange={(e) => setMinPrice(e.target.value)}
                className="input-field text-sm p-2"
              />
              <input
                type="number"
                placeholder="Max"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                className="input-field text-sm p-2"
              />
            </div>
          </div>

          {/* Rating */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-surface-500">Minimum Rating</h3>
            <div className="space-y-1">
              {[4, 3, 2].map((stars) => (
                <button
                  key={stars}
                  onClick={() => setMinRating(stars.toString())}
                  className={`w-full flex items-center space-x-2 py-2 px-3 rounded-lg text-sm transition-colors ${
                    minRating === stars.toString() ? 'bg-brand-50 text-brand-700 font-semibold' : 'text-surface-600 hover:bg-surface-50'
                  }`}
                >
                  <div className="flex items-center space-x-0.5">
                    {Array(stars).fill(0).map((_, i) => (
                      <Star key={i} className="h-4 w-4 fill-amber-400 text-amber-400" />
                    ))}
                    {Array(5 - stars).fill(0).map((_, i) => (
                      <Star key={i} className="h-4 w-4 text-surface-200" />
                    ))}
                  </div>
                  <span className="text-xs font-medium">& Up</span>
                </button>
              ))}
            </div>
          </div>

        </aside>

        {/* Product Cards Grid */}
        <div className="flex-1 space-y-8">
          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {Array(6).fill(0).map((_, idx) => (
                <div key={idx} className="h-96 bg-surface-100 rounded-xl animate-pulse"></div>
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="card rounded-2xl p-16 text-center border-dashed border-2 border-surface-200 bg-surface-50">
              <span className="text-6xl select-none">🔍</span>
              <h3 className="text-xl font-bold text-surface-900 mt-4">No results found</h3>
              <p className="text-surface-500 text-sm mt-2 max-w-sm mx-auto">We couldn't find any products matching your filters. Try adjusting your search criteria.</p>
              <button onClick={handleClearFilters} className="mt-6 btn-primary">
                Clear Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {products.map((prod) => (
                <div key={prod.id} className="w-full h-full card flex flex-col relative group">
                                     {/* Badges */}
                    <div className="absolute top-3 left-3 z-10 flex flex-col gap-1.5">
                      {prod.isTopDeal && (
                        <div className="bg-brand-500 text-white text-[10px] font-bold px-2 py-1 rounded shadow-sm flex items-center gap-1 uppercase tracking-wider">
                          <Flame className="h-3 w-3" />
                          <span>TOP DEAL</span>
                        </div>
                      )}
                      {prod.averageRating > 4.7 && (
                        <div className="bg-emerald-500 text-white text-[10px] font-bold px-2 py-1 rounded shadow-sm flex items-center gap-1 uppercase tracking-wider">
                          <Star className="h-3 w-3 fill-current" />
                          <span>TOP RATED</span>
                        </div>
                      )}
                    </div>

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
                          {prod.isTopDeal && prod.discountPercentage > 0 && (
                            <span className="text-xs text-surface-400 line-through">${prod.price.toFixed(2)}</span>
                          )}
                          <span className="text-lg font-extrabold text-surface-900">
                            ${prod.isTopDeal && prod.discountPercentage > 0 
                              ? (prod.price * (1 - prod.discountPercentage / 100)).toFixed(2) 
                              : prod.price}
                          </span>
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
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center space-x-4 pt-8 border-t border-surface-200 mt-8">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="p-2 bg-white border border-surface-300 rounded-lg text-surface-600 hover:text-brand-600 hover:border-brand-300 disabled:opacity-50 disabled:pointer-events-none transition-colors shadow-sm"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <span className="text-sm font-semibold text-surface-600">
                Page <span className="text-surface-900">{page + 1}</span> of <span className="text-surface-900">{totalPages}</span>
              </span>
              <button
                disabled={page === totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="p-2 bg-white border border-surface-300 rounded-lg text-surface-600 hover:text-brand-600 hover:border-brand-300 disabled:opacity-50 disabled:pointer-events-none transition-colors shadow-sm"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          )}

        </div>

      </div>

    </div>
  )
}

export default Catalog
