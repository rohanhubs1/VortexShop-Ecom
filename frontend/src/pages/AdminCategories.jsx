import toast from 'react-hot-toast'
import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { Plus, Edit2, Trash2, X } from 'lucide-react'

const AdminCategories = () => {
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)

  // Modals state
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId] = useState(null)
  
  // Fields state
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const loadCategories = async () => {
    setLoading(true)
    try {
      const res = await API.get('/categories')
      setCategories(res.data)
    } catch (err) {
      console.error('Failed to load categories', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategories()
  }, [])

  const handleFormSubmit = async (e) => {
    e.preventDefault()
    try {
      const payload = { name, description }
      if (editId) {
        await API.put(`/admin/categories/${editId}`, payload)
      } else {
        await API.post('/admin/categories', payload)
      }
      handleCloseModal()
      loadCategories()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save category')
    }
  }

  const handleEditClick = (cat) => {
    setEditId(cat.id)
    setName(cat.name)
    setDescription(cat.description || '')
    setShowModal(true)
  }

  const handleDeleteClick = async (id) => {
    if (!window.confirm('Delete this category? This might cause database reference constraints on products.')) return
    try {
      await API.delete(`/admin/categories/${id}`)
      loadCategories()
    } catch (err) {
      toast.error('Failed to delete category')
    }
  }

  const handleOpenAddModal = () => {
    setEditId(null)
    setName('')
    setDescription('')
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setEditId(null)
    setName('')
    setDescription('')
    setShowModal(false)
  }

  return (
    <div className="py-6 space-y-6">
      
      <div className="flex justify-between items-center border-b border-surface-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">Product Categories</h1>
          <p className="text-surface-500 text-sm mt-0.5 font-medium">Manage hierarchical subdivisions of your active product listings</p>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="btn-primary py-2.5 px-4 text-sm flex items-center space-x-1.5"
        >
          <Plus className="h-4.5 w-4.5" />
          <span>New Category</span>
        </button>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {categories.map((cat) => (
            <div key={cat.id} className="card p-6 rounded-2xl bg-white border border-surface-200 hover:border-brand-300 flex flex-col justify-between h-48 shadow-sm transition-colors relative">
              <div>
                <span className="text-[10px] text-brand-600 uppercase tracking-widest font-extrabold block mb-1">ID: {cat.id}</span>
                <h3 className="font-bold text-lg text-surface-900">{cat.name}</h3>
                <p className="text-sm text-surface-600 mt-2 line-clamp-2 font-medium">{cat.description || 'No description provided.'}</p>
              </div>
              
              <div className="flex justify-end space-x-2 pt-4 border-t border-surface-100 mt-4">
                <button
                  onClick={() => handleEditClick(cat)}
                  className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-brand-600 hover:border-brand-200 transition-colors shadow-sm"
                >
                  <Edit2 className="h-4 w-4" />
                </button>
                <button
                  onClick={() => handleDeleteClick(cat.id)}
                  className="p-2 bg-white border border-surface-200 rounded-lg text-surface-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors shadow-sm"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Form */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-surface-900/40 backdrop-blur-sm p-4">
          <div className="bg-white border border-surface-200 rounded-2xl w-full max-w-md p-6 shadow-xl space-y-6 relative">
            
            <div className="flex justify-between items-center border-b border-surface-100 pb-4">
              <h3 className="font-bold text-lg text-surface-900">{editId ? 'Edit Category' : 'Create Category'}</h3>
              <button onClick={handleCloseModal} className="text-surface-400 hover:text-surface-900 bg-surface-50 hover:bg-surface-100 rounded-full p-1 transition-colors focus:outline-none">
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div className="space-y-1">
                <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Category Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Workstations"
                  className="input-field py-2.5 text-sm"
                />
              </div>

              <div className="space-y-1">
                <label className="block text-xs font-bold text-surface-600 uppercase tracking-wider mb-1">Description</label>
                <textarea
                  rows="3"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="A short summary detailing category features..."
                  className="input-field py-2.5 text-sm"
                ></textarea>
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
                  Save Category
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  )
}

export default AdminCategories
