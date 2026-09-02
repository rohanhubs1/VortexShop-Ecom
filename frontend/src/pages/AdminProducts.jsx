import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { getImageUrl } from '../utils/imageHelper'
import { Plus, Edit2, Trash2, X, Upload, ChevronLeft, ChevronRight, Search, Flame } from 'lucide-react'

const AdminProducts = () => {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [deleteConfirmId, setDeleteConfirmId] = useState(null)

  // Filters
  const [keyword, setKeyword] = useState('')
  const [filterCategoryId, setFilterCategoryId] = useState('')
  const [sort, setSort] = useState('createdAt,desc')
  const [filterTopDeals, setFilterTopDeals] = useState(false)

  // Modal forms
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId] = useState(null)
  
  // Form fields
  const [name, setName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [price, setPrice] = useState('')
  const [stockQuantity, setStockQuantity] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [isTopDeal, setIsTopDeal] = useState(false)
  const [discountPercentage, setDiscountPercentage] = useState(0)
  const [offerExpiryDate, setOfferExpiryDate] = useState('')
  const [uploading, setUploading] = useState(false)

  const loadData = async () => {
    setLoading(true)
    try {
      let url = `/products?page=${page}&size=10&sort=${sort}`
      if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
      if (filterCategoryId) url += `&categoryId=${filterCategoryId}`
      if (filterTopDeals) url += `&isTopDeal=true`
      
      const [prodRes, catRes] = await Promise.all([
        API.get(url),
        API.get('/categories')
      ])
      setProducts(prodRes.data.content)
      setTotalPages(prodRes.data.totalPages)
      setCategories(catRes.data)
    } catch (err) {
      console.error('Failed to load products list', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      loadData()
    }, 500)
    return () => clearTimeout(delayDebounceFn)
  }, [keyword, page, filterCategoryId, sort, filterTopDeals])

  const handleImageUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setUploading(true)
    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await API.post('/admin/products/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setImageUrl(res.data.imageUrl)
    } catch (err) {
      toast.error('Failed to upload image')
    } finally {
      setUploading(false)
    }
  }

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    try {
      const payload = {
        name,
        categoryId: parseInt(categoryId),
        description,
        price: parseFloat(price),
        stockQuantity: parseInt(stockQuantity),
        imageUrl,
        isTopDeal,
        discountPercentage: parseInt(discountPercentage || 0),
        offerExpiryDate: offerExpiryDate ? (offerExpiryDate.length === 16 ? offerExpiryDate + ":00" : offerExpiryDate) : null
      }

      if (editId) {
        await API.put(`/admin/products/${editId}`, payload)
      } else {
        await API.post('/admin/products', payload)
      }

      handleCloseModal()
      loadData()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save product details')
    }
  }

  const handleEditClick = (prod) => {
    setEditId(prod.id)
    setName(prod.name)
    setCategoryId(prod.categoryId.toString())
    setDescription(prod.description)
    setPrice(prod.price)
    setStockQuantity(prod.stockQuantity)
    setImageUrl(prod.imageUrl || '')
    setIsTopDeal(prod.isTopDeal || false)
    setDiscountPercentage(prod.discountPercentage || 0)
    setOfferExpiryDate(prod.offerExpiryDate ? prod.offerExpiryDate.substring(0, 16) : '')
    setShowModal(true)
  }

  const handleDeleteClick = (prodId) => {
    setDeleteConfirmId(prodId)
  }

  const confirmDelete = async () => {
    if (!deleteConfirmId) return
    try {
      await API.delete(`/admin/products/${deleteConfirmId}`)
      loadData()
    } catch (err) {
      toast.error('Failed to delete product')
    } finally {
      setDeleteConfirmId(null)
    }
  }

  const handleOpenAddModal = () => {
    setEditId(null)
    setName('')
    setCategoryId(categories.length > 0 ? categories[0].id.toString() : '')
    setDescription('')
    setPrice('')
    setStockQuantity('')
    setImageUrl('')
    setIsTopDeal(false)
    setDiscountPercentage(0)
    setOfferExpiryDate('')
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setEditId(null)
    setName('')
    setDescription('')
    setPrice('')
    setStockQuantity('')
    setImageUrl('')
    setIsTopDeal(false)
    setDiscountPercentage(0)
    setOfferExpiryDate('')
    setShowModal(false)
  }

  return (
    <div className="py-6 space-y-6">
      
      <div className="flex justify-between items-center border-b border-surface-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-surface-200 tracking-tight">Manage Products</h1>
          <p className="text-surface-500 text-sm mt-0.5 font-medium">Add, edit, upload images, or delete items from catalog</p>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="btn-primary py-2.5 px-4 text-sm flex items-center space-x-1.5"
        >
          <Plus className="h-4.5 w-4.5" />
          <span>Add Product</span>
        </button>
      </div>

      {/* Filters Bar */}
      <div className="flex flex-col md:flex-row justify-between items-center gap-4 bg-surface-50 p-4 rounded-xl border border-surface-200">
        <div className="w-full md:w-1/3 relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-surface-400" />
          </div>
          <input
            type="text"
            placeholder="Search products by name..."
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
            className="input-field pl-10 py-2.5 text-sm w-full bg-white"
          />
        </div>
        <div className="flex items-center space-x-4 w-full md:w-auto">
          <select
            value={filterCategoryId}
            onChange={(e) => { setFilterCategoryId(e.target.value); setPage(0); }}
            className="input-field py-2.5 text-sm w-full md:w-auto bg-white"
          >
            <option value="">All Categories</option>
            {categories.map(cat => (
              <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
          </select>
          <select
            value={sort}
            onChange={(e) => { setSort(e.target.value); setPage(0); }}
            className="input-field py-2.5 text-sm w-full md:w-auto bg-white"
          >
            <option value="createdAt,desc">Latest Arrivals</option>
            <option value="salesCount,desc">Most Ordered</option>
            <option value="price,asc">Price: Low to High</option>
            <option value="price,desc">Price: High to Low</option>
            <option value="stockQuantity,desc">Stock: Highest First</option>
            <option value="stockQuantity,asc">Stock: Lowest First</option>
          </select>
          <button
            type="button"
            onClick={() => { setFilterTopDeals(!filterTopDeals); setPage(0); }}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold border flex items-center justify-center gap-1.5 transition-all w-full md:w-auto ${
              filterTopDeals 
                ? 'bg-brand-500 text-white border-brand-500 shadow-sm' 
                : 'bg-white text-surface-600 border-surface-200 hover:bg-surface-50 shadow-sm'
            }`}
          >
            <Flame className={`h-4 w-4 ${filterTopDeals ? 'text-white' : 'text-brand-500'}`} />
            <span>Top Deals Only</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="card rounded-2xl border border-surface-200 overflow-hidden shadow-sm bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-surface-50 text-surface-500 border-b border-surface-200">
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Preview</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Name</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Category</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Price</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Stock</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Sales</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-100">
                {products.map((prod) => (
                  <tr key={prod.id} className="hover:bg-brand-50/30 transition-colors">
                    <td className="p-4">
                      {prod.imageUrl ? (
                         <img src={getImageUrl(prod.imageUrl)} alt={prod.name} className="h-10 w-10 object-cover rounded-lg border border-surface-200" />
                      ) : (
                         <div className="h-10 w-10 bg-surface-50 rounded-lg border border-surface-200 flex items-center justify-center text-lg shadow-inner">
                           {prod.categoryId === 1 ? '💻' : prod.categoryId === 2 ? '🧥' : prod.categoryId === 3 ? '🏺' : '📚'}
                         </div>
                      )}
                    </td>
                    <td className="p-4 font-bold text-surface-900 max-w-[200px] truncate">{prod.name}</td>
                    <td className="p-4 text-surface-600 font-medium">{prod.categoryName}</td>
                    <td className="p-4">
                      <div className="flex flex-col">
                        {prod.isTopDeal && prod.discountPercentage > 0 ? (
                          <>
                            <span className="font-extrabold text-surface-900">${(prod.price * (1 - prod.discountPercentage / 100)).toFixed(2)}</span>
                            <span className="text-[10px] text-red-500 font-bold line-through">${prod.price.toFixed(2)} (-{prod.discountPercentage}%)</span>
                          </>
                        ) : (
                          <span className="font-bold text-surface-900">${prod.price.toFixed(2)}</span>
                        )}
                      </div>
                    </td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded tracking-wider text-[10px] font-extrabold uppercase ${
                        prod.stockQuantity > 10
                          ? 'bg-emerald-100 text-emerald-800'
                          : prod.stockQuantity > 0
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {prod.stockQuantity} Left
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-surface-600 font-bold text-sm bg-surface-100 px-2.5 py-1 rounded">
                        {prod.salesCount || 0} Sold
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex justify-end space-x-2">
                        <button onClick={() => handleEditClick(prod)} className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-brand-600 hover:border-brand-200 transition-colors shadow-sm">
                          <Edit2 className="h-4 w-4" />
                        </button>
                        <button onClick={() => handleDeleteClick(prod.id)} className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors shadow-sm">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center space-x-4 py-4 bg-surface-50 border-t border-surface-200">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-surface-900 disabled:opacity-50 shadow-sm"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="text-xs font-bold text-surface-500 uppercase tracking-wider">Page {page + 1} of {totalPages}</span>
              <button
                disabled={page === totalPages - 1}
                onClick={() => setPage(page + 1)}
                className="p-1.5 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-surface-900 disabled:opacity-50 shadow-sm"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>
      )}

      {/* Modal Form */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-surface-900/40 backdrop-blur-sm p-4">
          <div className="bg-white border border-surface-200 rounded-2xl w-full max-w-lg p-6 shadow-xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            
            <div className="flex justify-between items-center border-b border-surface-100 pb-4">
              <h3 className="font-bold text-lg text-surface-900">{editId ? 'Edit Product Details' : 'Add New Product'}</h3>
              <button onClick={handleCloseModal} className="text-surface-400 hover:text-surface-900 bg-surface-50 hover:bg-surface-100 rounded-full p-1 transition-colors focus:outline-none">
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Product Title</label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g. Apex Watch"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Category</label>
                  <select
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    className="input-field py-2.5 text-sm"
                  >
                    {categories.map(cat => (
                      <option key={cat.id} value={cat.id}>{cat.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="space-y-1">
                <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Description</label>
                <textarea
                  required
                  rows="3"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Tell customers about features, hardware specifications..."
                  className="input-field py-2.5 text-sm"
                ></textarea>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    placeholder="199.99"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Initial Stock</label>
                  <input
                    type="number"
                    required
                    value={stockQuantity}
                    onChange={(e) => setStockQuantity(e.target.value)}
                    placeholder="50"
                    className="input-field py-2.5 text-sm"
                  />
                </div>
              </div>

              {/* Top Deal Settings */}
              <div className="border border-surface-200 rounded-xl p-4 space-y-4 bg-surface-50">
                <label className="flex items-center space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isTopDeal}
                    onChange={(e) => setIsTopDeal(e.target.checked)}
                    className="h-4 w-4 text-brand-600 focus:ring-brand-500 border-surface-300 rounded"
                  />
                  <span className="text-sm font-bold text-surface-900">Mark as Top Deal</span>
                </label>
                
                {isTopDeal && (
                  <div className="grid grid-cols-2 gap-4 pt-2 border-t border-surface-200">
                    <div className="space-y-1">
                      <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Discount (%)</label>
                      <input
                        type="number"
                        min="1"
                        max="99"
                        value={discountPercentage}
                        onChange={(e) => setDiscountPercentage(e.target.value)}
                        placeholder="20"
                        className="input-field py-2.5 text-sm"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Expiry Date</label>
                      <input
                        type="datetime-local"
                        value={offerExpiryDate}
                        onChange={(e) => setOfferExpiryDate(e.target.value)}
                        className="input-field py-2.5 text-sm"
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Mock Image Upload */}
              <div className="space-y-2 border-t border-surface-200 pt-4">
                <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider">Product Image</label>
                
                <div className="flex items-center space-x-4">
                  <div className="flex-1">
                    <label className="flex flex-col items-center justify-center border-2 border-dashed border-surface-300 rounded-xl p-4 bg-surface-50 hover:bg-surface-100 hover:border-brand-300 cursor-pointer transition-colors relative shadow-inner">
                      {uploading ? (
                        <span className="text-sm font-bold text-brand-600">Uploading file...</span>
                      ) : (
                        <>
                          <Upload className="h-6 w-6 text-brand-500 mb-2" />
                          <span className="text-xs font-bold text-surface-500 uppercase tracking-wider">Upload Product Image</span>
                        </>
                      )}
                      <input type="file" onChange={handleImageUpload} className="hidden" accept="image/*" />
                    </label>
                  </div>
                  {imageUrl && (
                    <div className="h-20 w-20 bg-white rounded-xl border border-surface-200 flex items-center justify-center text-[10px] text-surface-600 font-bold shrink-0 p-1 shadow-sm overflow-hidden">
                       <img src={getImageUrl(imageUrl)} alt="Uploaded" className="object-cover h-full w-full" />
                    </div>
                  )}
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-surface-200 mt-2">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="btn-secondary py-2"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary py-2"
                >
                  Save Product
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirmId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-surface-900/40 backdrop-blur-sm p-4">
          <div className="bg-white border border-surface-200 rounded-2xl w-full max-w-sm p-6 shadow-xl space-y-6">
            <h3 className="font-bold text-lg text-surface-900">Confirm Deletion</h3>
            <p className="text-sm text-surface-600">Are you sure you want to completely delete this product from the catalog? This action cannot be undone.</p>
            <div className="flex justify-end space-x-3 pt-4 border-t border-surface-200 mt-2">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="btn-secondary py-2"
              >
                Cancel
              </button>
              <button
                onClick={confirmDelete}
                className="btn-primary py-2 bg-red-600 hover:bg-red-700 focus:ring-red-500"
              >
                Delete Product
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}

export default AdminProducts
