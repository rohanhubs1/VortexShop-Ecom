import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { getImageUrl } from '../utils/imageHelper'
import { MessageSquare, CheckCircle, Upload } from 'lucide-react'
import toast from 'react-hot-toast'

const AdminSupport = () => {
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)

  // Thread Reply State
  const [expandedTicketId, setExpandedTicketId] = useState(null)
  const [replyText, setReplyText] = useState('')
  const [replyFile, setReplyFile] = useState(null)
  const [replyingId, setReplyingId] = useState(null)

  const fetchTickets = async () => {
    try {
      const res = await API.get('/admin/support')
      setTickets(res.data)
    } catch (err) {
      toast.error('Failed to load support tickets')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchTickets()
  }, [])

  const handleResolve = async (id) => {
    try {
      await API.put(`/admin/support/${id}/resolve`)
      toast.success('Ticket marked as resolved!')
      fetchTickets()
    } catch (err) {
      toast.error('Failed to resolve ticket')
    }
  }

  const handleReplySubmit = async (e, ticketId) => {
    e.preventDefault()
    if (!replyText.trim() && !replyFile) return

    setReplyingId(ticketId)
    try {
      let attachmentUrl = null
      if (replyFile) {
        const formData = new FormData()
        formData.append('file', replyFile)
        const uploadRes = await API.post('/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        attachmentUrl = uploadRes.data.url
      }

      await API.post(`/admin/support/${ticketId}/reply`, {
        message: replyText,
        attachmentUrl
      })

      toast.success('Reply sent successfully!')
      setReplyText('')
      setReplyFile(null)
      fetchTickets()
    } catch (err) {
      toast.error('Failed to send reply. Please try again.')
    } finally {
      setReplyingId(null)
    }
  }

  return (
    <div className="py-6 space-y-6">
      <div className="border-b border-surface-200 pb-5">
        <h1 className="text-3xl font-extrabold text-surface-200 tracking-tight">Customer Support</h1>
        <p className="text-surface-500 text-sm mt-0.5 font-medium">Manage and resolve customer inquiries</p>
      </div>

      {loading ? (
        <div className="animate-pulse h-40 bg-surface-100 rounded-xl"></div>
      ) : (
        <div className="grid gap-4">
          {tickets.length === 0 ? (
            <div className="text-center py-10 bg-surface-50 rounded-2xl border border-dashed border-surface-200">
              <MessageSquare className="h-10 w-10 text-surface-300 mx-auto mb-3" />
              <p className="text-surface-500 font-medium">No support tickets found.</p>
            </div>
          ) : (
            tickets.map(ticket => (
              <div key={ticket.id} className="bg-white p-6 rounded-2xl border border-surface-200 shadow-sm flex flex-col md:flex-row gap-6">
                <div className="flex-1 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-bold text-surface-900">{ticket.subject}</h3>
                    <span className={`px-2.5 py-1 rounded text-xs font-extrabold uppercase ${ticket.status === 'OPEN' ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'}`}>
                      {ticket.status}
                    </span>
                  </div>
                  <div className="text-sm font-medium text-surface-500">
                    From: <span className="text-surface-700 font-bold">{ticket.customerName}</span> ({ticket.customerEmail})
                  </div>
                  <div className="bg-surface-50 p-4 rounded-xl text-sm text-surface-700 border border-surface-100 whitespace-pre-wrap">
                    {ticket.message}
                  </div>
                  {ticket.attachmentUrl && (
                    <div className="pt-2">
                      <a href={getImageUrl(ticket.attachmentUrl)} target="_blank" rel="noopener noreferrer" className="inline-flex items-center text-xs font-bold bg-brand-50 text-brand-600 px-3 py-1.5 rounded-lg hover:bg-brand-100 transition-colors">
                        <Upload className="h-3.5 w-3.5 mr-1.5" /> View Attachment
                      </a>
                    </div>
                  )}
                  <div className="text-xs text-surface-400 font-medium pt-2">
                    Opened: {new Date(ticket.createdAt).toLocaleString()}
                  </div>

                  {/* Collapsible Chat Thread */}
                  <div className="mt-4 pt-4 border-t border-surface-150">
                    <button 
                      type="button"
                      onClick={() => {
                        setExpandedTicketId(expandedTicketId === ticket.id ? null : ticket.id)
                        setReplyText('')
                        setReplyFile(null)
                      }}
                      className="text-xs font-bold text-brand-600 hover:underline flex items-center gap-1.5"
                    >
                      <MessageSquare className="h-3.5 w-3.5" />
                      {expandedTicketId === ticket.id ? 'Hide Chat History' : `View Conversation Thread (${(ticket.messages || []).length + 1})`}
                    </button>

                    {expandedTicketId === ticket.id && (
                      <div className="mt-4 space-y-4">
                        {/* Chat Messages Log */}
                        <div className="bg-surface-50 p-4 rounded-xl border border-surface-200 max-h-80 overflow-y-auto space-y-3 flex flex-col">
                          
                          {/* Original customer request (shown on the left) */}
                          <div className="max-w-[80%] self-start">
                            <div className="bg-white text-surface-800 border border-surface-200 p-3 rounded-2xl rounded-tl-none shadow-sm text-sm">
                              <p className="whitespace-pre-wrap">{ticket.message}</p>
                              {ticket.attachmentUrl && (
                                <div className="mt-2 pt-2 border-t border-surface-150">
                                  <a href={getImageUrl(ticket.attachmentUrl)} target="_blank" rel="noopener noreferrer" className="inline-flex items-center text-xs font-bold text-brand-600 hover:underline">
                                    <Upload className="h-3 w-3 mr-1" /> View Attachment
                                  </a>
                                </div>
                              )}
                            </div>
                            <span className="text-[10px] text-surface-400 font-medium mt-1 block text-left">
                              {ticket.customerName} • {new Date(ticket.createdAt).toLocaleString()}
                            </span>
                          </div>

                          {/* Thread messages */}
                          {(ticket.messages || []).map((msg) => {
                            const isMe = msg.senderRole === 'ADMIN';
                            return (
                              <div 
                                key={msg.id} 
                                className={`max-w-[80%] ${isMe ? 'self-end' : 'self-start'}`}
                              >
                                <div 
                                  className={`p-3 rounded-2xl shadow-sm text-sm ${
                                    isMe 
                                      ? 'bg-brand-600 text-white rounded-tr-none' 
                                      : 'bg-white text-surface-800 border border-surface-200 rounded-tl-none'
                                  }`}
                                >
                                  <p className="whitespace-pre-wrap">{msg.message}</p>
                                  {msg.attachmentUrl && (
                                    <div className={`mt-2 pt-2 border-t ${isMe ? 'border-brand-500' : 'border-surface-150'}`}>
                                      <a 
                                        href={getImageUrl(msg.attachmentUrl)} 
                                        target="_blank" 
                                        rel="noopener noreferrer" 
                                        className={`inline-flex items-center text-xs font-bold hover:underline ${isMe ? 'text-brand-100' : 'text-brand-600'}`}
                                      >
                                        <Upload className="h-3 w-3 mr-1" /> View Attachment
                                      </a>
                                    </div>
                                  )}
                                </div>
                                <span className={`text-[10px] text-surface-400 font-medium mt-1 block ${isMe ? 'text-right' : 'text-left'}`}>
                                  <span className="font-bold mr-1">{isMe ? 'You (Admin)' : ticket.customerName}</span> • {new Date(msg.createdAt).toLocaleString()}
                                </span>
                              </div>
                            )
                          })}
                        </div>

                        {/* Reply field */}
                        <form onSubmit={(e) => handleReplySubmit(e, ticket.id)} className="space-y-2.5">
                          <div className="flex gap-2">
                            <input 
                              type="text" 
                              required
                              value={replyText}
                              onChange={(e) => setReplyText(e.target.value)}
                              placeholder="Type your reply response..." 
                              className="input-field flex-1 py-2 px-3 text-sm rounded-xl"
                            />
                            <button 
                              type="submit" 
                              disabled={replyingId === ticket.id}
                              className="btn-primary py-2 px-4 rounded-xl text-sm shrink-0"
                            >
                              {replyingId === ticket.id ? 'Sending...' : 'Send'}
                            </button>
                          </div>
                          
                          <div className="flex items-center gap-3">
                            <label className="cursor-pointer bg-surface-100 hover:bg-surface-200 text-surface-700 px-3 py-1.5 rounded-lg text-xs font-bold flex items-center transition-colors">
                              <Upload className="h-3.5 w-3.5 mr-1" />
                              {replyFile ? replyFile.name : 'Attach Image (Optional)'}
                              <input 
                                type="file" 
                                className="hidden" 
                                accept="image/*" 
                                onChange={e => setReplyFile(e.target.files[0])} 
                              />
                            </label>
                            {replyFile && (
                              <button 
                                type="button" 
                                onClick={() => setReplyFile(null)} 
                                className="text-[10px] text-red-600 font-bold hover:underline"
                              >
                                Remove
                              </button>
                            )}
                          </div>
                        </form>
                      </div>
                    )}
                  </div>
                </div>
                
                {ticket.status === 'OPEN' && (
                  <div className="flex items-center justify-end md:items-start">
                    <button 
                      onClick={() => handleResolve(ticket.id)}
                      className="btn-primary py-2 px-4 flex items-center shadow-sm"
                    >
                      <CheckCircle className="h-4 w-4 mr-2" /> Resolve
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export default AdminSupport
