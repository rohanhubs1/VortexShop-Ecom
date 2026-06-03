import React, { createContext, useState, useEffect, useContext } from 'react'
import API from '../api/axios'
import { useAuth } from './AuthContext'

const CartContext = createContext(null)

export const CartProvider = ({ children }) => {
  const { isAuthenticated, user } = useAuth()
  const [cart, setCart] = useState(null)
  const [wishlist, setWishlist] = useState(null)
  const [cartLoading, setCartLoading] = useState(false)
  const [wishlistLoading, setWishlistLoading] = useState(false)

  const fetchCart = async () => {
    if (!isAuthenticated) return
    setCartLoading(true)
    try {
      const res = await API.get('/cart')
      setCart(res.data)
    } catch (err) {
      console.error('Failed to fetch cart', err)
    } finally {
      setCartLoading(false)
    }
  }

  const fetchWishlist = async () => {
    if (!isAuthenticated) return
    setWishlistLoading(true)
    try {
      const res = await API.get('/wishlist')
      setWishlist(res.data)
    } catch (err) {
      console.error('Failed to fetch wishlist', err)
    } finally {
      setWishlistLoading(false)
    }
  }

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart()
      fetchWishlist()
    } else {
      setCart(null)
      setWishlist(null)
    }
  }, [isAuthenticated, user])

  const requireAuth = () => {
    if (!isAuthenticated) {
      window.location.href = '/login'
      throw new Error('Please log in to continue')
    }
  }

  const addToCart = async (productId, quantity = 1) => {
    requireAuth()
    
    // Optimistic UI update
    const previousCart = { ...cart }
    
    try {
      const res = await API.post('/cart', { productId, quantity })
      setCart(res.data)
      return res.data
    } catch (err) {
      setCart(previousCart)
      throw err.response?.data?.message || 'Failed to add item to cart'
    }
  }

  const updateCartQuantity = async (productId, quantity) => {
    if (quantity < 1) return;
    requireAuth()
    
    // Optimistic UI Update
    const previousCart = { ...cart }
    if (cart && cart.items) {
      const updatedItems = cart.items.map(item => 
        item.productId === productId ? { ...item, quantity: quantity, subTotal: item.productPrice * quantity } : item
      )
      const newTotal = updatedItems.reduce((acc, item) => acc + item.subTotal, 0)
      setCart({ ...cart, items: updatedItems, totalAmount: newTotal })
    }

    try {
      const res = await API.put(`/cart/items/${productId}`, { quantity })
      setCart(res.data)
    } catch (err) {
      setCart(previousCart)
      throw err.response?.data?.message || 'Failed to update quantity'
    }
  }

  const removeFromCart = async (productId) => {
    requireAuth()
    
    // Optimistic UI Update
    const previousCart = { ...cart }
    if (cart && cart.items) {
      const updatedItems = cart.items.filter(item => item.productId !== productId)
      const newTotal = updatedItems.reduce((acc, item) => acc + item.subTotal, 0)
      setCart({ ...cart, items: updatedItems, totalAmount: newTotal })
    }

    try {
      const res = await API.delete(`/cart/items/${productId}`)
      setCart(res.data)
    } catch (err) {
      setCart(previousCart)
      throw err.response?.data?.message || 'Failed to remove item'
    }
  }

  // Wishlist
  const addToWishlist = async (productId) => {
    requireAuth()
    
    // Optimistic
    const prevWishlist = { ...wishlist }
    setWishlist({ ...wishlist, products: [...(wishlist?.products || []), { id: productId }] })
    
    try {
      const res = await API.post(`/wishlist/products/${productId}`)
      setWishlist(res.data)
    } catch (err) {
      setWishlist(prevWishlist)
      throw err.response?.data?.message || 'Failed to add to wishlist'
    }
  }

  const removeFromWishlist = async (productId) => {
    try {
      const res = await API.delete(`/wishlist/products/${productId}`)
      setWishlist(res.data)
    } catch (err) {
      throw err.response?.data?.message || 'Failed to remove from wishlist'
    }
  }

  const isInWishlist = (productId) => {
    return wishlist?.products?.some(p => p.id === productId) || false
  }

  const cartCount = cart?.items?.reduce((acc, item) => acc + item.quantity, 0) || 0

  return (
    <CartContext.Provider value={{
      cart,
      wishlist,
      cartLoading,
      wishlistLoading,
      addToCart,
      updateCartQuantity,
      removeFromCart,
      fetchCart,
      addToWishlist,
      removeFromWishlist,
      isInWishlist,
      cartCount
    }}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => useContext(CartContext)
