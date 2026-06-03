import React, { createContext, useState, useEffect, useContext } from 'react'
import API from '../api/axios'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const initializeAuth = async () => {
      const token = localStorage.getItem('token')
      if (token) {
        try {
          const res = await API.get('/auth/me')
          setUser(res.data)
        } catch (err) {
          console.error('Session initialization failed', err)
          localStorage.removeItem('token')
        }
      }
      setLoading(false)
    }
    initializeAuth()
  }, [])

  const login = async (email, password) => {
    setLoading(true)
    try {
      const res = await API.post('/auth/login', { email, password })
      const { token, ...userData } = res.data
      localStorage.setItem('token', token)
      setUser(userData)
      return userData
    } catch (err) {
      throw err.response?.data?.message || 'Login failed'
    } finally {
      setLoading(false)
    }
  }

  const register = async (firstName, lastName, email, password, phone) => {
    setLoading(true)
    try {
      await API.post('/auth/register', { firstName, lastName, email, password, phone })
    } catch (err) {
      throw err.response?.data?.message || 'Registration failed'
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
  }

  const isAdmin = user?.roles?.includes('ROLE_ADMIN') || false
  const isAuthenticated = !!user

  return (
    <AuthContext.Provider value={{ user, loading, isAuthenticated, isAdmin, login, register, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
