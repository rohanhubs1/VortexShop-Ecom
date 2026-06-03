import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { Users, Mail, Phone, Calendar } from 'lucide-react'

const AdminCustomers = () => {
  const [customers, setCustomers] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const res = await API.get('/admin/customers')
        setCustomers(res.data)
      } catch (err) {
        console.error('Failed to load customers list', err)
      } finally {
        setLoading(false)
      }
    }
    fetchCustomers()
  }, [])

  return (
    <div className="py-6 space-y-6">
      
      <div className="border-b border-surface-200 pb-5">
        <h1 className="text-3xl font-extrabold text-surface-900 tracking-tight">Registered Customers</h1>
        <p className="text-surface-500 text-sm mt-0.5 font-medium">List of users registered under the customer security role</p>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="card rounded-2xl border border-surface-200 overflow-hidden shadow-sm bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-surface-50 text-surface-500 border-b border-surface-200">
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">ID</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Name</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Email Address</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Phone</th>
                  <th className="p-4 font-bold uppercase tracking-wider text-xs">Security Roles</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-100">
                {customers.map((cust) => (
                  <tr key={cust.id} className="hover:bg-brand-50/30 transition-colors">
                    <td className="p-4 font-bold text-surface-400">#{cust.id}</td>
                    <td className="p-4 font-bold text-surface-900">{cust.firstName} {cust.lastName}</td>
                    <td className="p-4">
                      <div className="flex items-center space-x-1.5 text-surface-600 font-medium">
                        <Mail className="h-4 w-4 text-surface-400" />
                        <span>{cust.email}</span>
                      </div>
                    </td>
                    <td className="p-4">
                      {cust.phone ? (
                        <div className="flex items-center space-x-1.5 text-surface-600 font-medium">
                          <Phone className="h-4 w-4 text-surface-400" />
                          <span>{cust.phone}</span>
                        </div>
                      ) : (
                        <span className="text-surface-400 italic text-xs font-medium">None provided</span>
                      )}
                    </td>
                    <td className="p-4">
                      <div className="flex flex-wrap gap-1">
                        {cust.roles.map(r => (
                          <span key={r} className="bg-brand-50 text-brand-700 px-2.5 py-1 rounded tracking-wider text-[10px] font-extrabold uppercase">
                            {r.replace('ROLE_', '')}
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  )
}

export default AdminCustomers
