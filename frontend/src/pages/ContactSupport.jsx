import React, { useState, useEffect } from 'react'
import API from '../api/axios'
import { getImageUrl } from '../utils/imageHelper'
import { MessageSquare, Upload, CheckCircle, Clock } from 'lucide-react'
import toast from 'react-hot-toast'

const ContactSupport = () => {
  const [tickets, setTickets] = useState([])
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)

  // Thread Reply State
  const [expandedTicketId, setExpandedTicketId] = useState(null)
  const [replyText, setReplyText] = useState('')
  const [replyFile, setReplyFile] = useState(null)
  const [replyingId, setReplyingId] = useState(null)

  const fetchTickets = async () => {
    try {
      const res = await API.get('/support')
      setTickets(res.data)
    } catch (err) {
      toast.error('Failed to load your support tickets')
    }
  }

  useEffect(() => {
    fetchTickets()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setUploading(true)
    try {
      let attachmentUrl = null
      if (file) {
        const formData = new FormData()
        formData.append('file', file)
        const uploadRes = await API.post('/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        attachmentUrl = uploadRes.data.url
      }
      
      await API.post('/support', { subject, message, attachmentUrl })
      toast.success('Support ticket submitted successfully!')
      setSubject('')
      setMessage('')
      setFile(null)
      fetchTickets()
    } catch (err) {
      toast.error('Failed to submit ticket. Please try again.')
    } finally {
      setUploading(false)
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

      await API.post(`/support/${ticketId}/reply`, {
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
    <div className="py-8 max-w-4xl mx-auto space-y-12">
      <div>
        <h1 className="text-3xl font-extrabold text-surface-300 mb-2">Contact Support</h1>
        <p className="text-surface-600 font-medium">We're here to help. Submit a ticket and our team will get back to you shortly.</p>
      </div>

      <div className="card p-6 bg-white border border-surface-200 shadow-sm rounded-2xl">
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-bold text-surface-700 mb-1.5">Subject</label>
            <input 
              type="text" 
              required 
              value={subject} 
              onChange={e => setSubject(e.target.value)} 
              placeholder="E.g., Order not received" 
              className="input-field" 
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-surface-700 mb-1.5">Message</label>
            <textarea 
              required 
              rows="4" 
              value={message} 
              onChange={e => setMessage(e.target.value)} 
              placeholder="Describe your issue in detail..." 
              className="input-field py-3" 
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-surface-700 mb-1.5">Attachment (Optional)</label>
            <div className="flex items-center gap-4">
              <label className="cursor-pointer bg-surface-100 hover:bg-surface-200 text-surface-700 px-4 py-2.5 rounded-xl text-sm font-bold flex items-center transition-colors">
                <Upload className="h-4 w-4 mr-2" />
                {file ? file.name : 'Choose File'}
                <input type="file" className="hidden" accept="image/*" onChange={e => setFile(e.target.files[0])} />
              </label>
              {file && (
                <button type="button" onClick={() => setFile(null)} className="text-xs text-red-600 font-bold hover:underline">Remove</button>
              )}
            </div>
          </div>
          <button type="submit" disabled={uploading} className="btn-primary w-full py-3">
            {uploading ? 'Submitting...' : 'Submit Ticket'}
          </button>
        </form>
      </div>

      <div className="space-y-4">
        <h2 className="text-xl font-extrabold text-surface-300">Your Recent Tickets</h2>
        {tickets.length === 0 ? (
          <div className="text-center py-10 bg-surface-50 rounded-2xl border border-dashed border-surface-200">
            <MessageSquare className="h-10 w-10 text-surface-300 mx-auto mb-3" />
            <p className="text-surface-500 font-medium">You haven't submitted any support tickets yet.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {tickets.map(ticket => (
              <div key={ticket.id} className="bg-white p-5 rounded-2xl border border-surface-200 shadow-sm">
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-bold text-surface-900">{ticket.subject}</h3>
                  {ticket.status === 'OPEN' ? (
                    <span className="flex items-center bg-amber-50 text-amber-700 px-2.5 py-1 rounded text-xs font-extrabold uppercase">
                      <Clock className="h-3 w-3 mr-1" /> Open
                    </span>
                  ) : (
                    <span className="flex items-center bg-emerald-50 text-emerald-700 px-2.5 py-1 rounded text-xs font-extrabold uppercase">
                      <CheckCircle className="h-3 w-3 mr-1" /> Resolved
                    </span>
                  )}
                </div>
                <p className="text-sm text-surface-600 whitespace-pre-wrap">{ticket.message}</p>
                {ticket.attachmentUrl && (
                  <div className="mt-3">
                    <a href={getImageUrl(ticket.attachmentUrl)} target="_blank" rel="noopener noreferrer" className="text-xs text-brand-600 font-bold hover:underline flex items-center">
                      <Upload className="h-3 w-3 mr-1" /> View Attachment
                    </a>
                  </div>
                )}
                
                {/* Collapsible Chat Thread */}
                <div className="mt-4 pt-4 border-t border-surface-100">
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
                    {expandedTicketId === ticket.id ? 'Hide Chat History' : `View Conversation (${(ticket.messages || []).length + 1})`}
                  </button>

                  {expandedTicketId === ticket.id && (
                    <div className="mt-4 space-y-4">
                      {/* Chat Messages Log */}
                      <div className="bg-surface-50 p-4 rounded-xl border border-surface-200 max-h-80 overflow-y-auto space-y-3 flex flex-col">
                        
                        {/* Original ticket request */}
                        <div className="max-w-[80%] self-end">
                          <div className="bg-brand-600 text-white p-3 rounded-2xl rounded-tr-none shadow-sm text-sm">
                            <p className="whitespace-pre-wrap">{ticket.message}</p>
                            {ticket.attachmentUrl && (
                              <div className="mt-2 pt-2 border-t border-brand-500">
                                <a href={getImageUrl(ticket.attachmentUrl)} target="_blank" rel="noopener noreferrer" className="inline-flex items-center text-xs font-bold text-brand-100 hover:underline">
                                  <Upload className="h-3 w-3 mr-1" /> View Attachment
                                </a>
                              </div>
                            )}
                          </div>
                          <span className="text-[10px] text-surface-400 font-medium mt-1 block text-right">
                            You • {new Date(ticket.createdAt).toLocaleString()}
                          </span>
                        </div>

                        {/* Thread messages */}
                        {(ticket.messages || []).map((msg) => {
                          const isMe = msg.senderRole === 'CUSTOMER';
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
                                <span className="font-bold mr-1">{isMe ? 'You' : 'Admin'}</span> • {new Date(msg.createdAt).toLocaleString()}
                              </span>
                            </div>
                          )
                        })}
                      </div>

                      {/* Reply field */}
                      {ticket.status === 'OPEN' ? (
                        <form onSubmit={(e) => handleReplySubmit(e, ticket.id)} className="space-y-2.5">
                          <div className="flex gap-2">
                            <input 
                              type="text" 
                              required
                              value={replyText}
                              onChange={(e) => setReplyText(e.target.value)}
                              placeholder="Type your message reply..." 
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
                      ) : (
                        <div className="bg-emerald-50 text-emerald-800 text-xs p-3.5 rounded-xl border border-emerald-200 text-center font-semibold">
                          This ticket is resolved. Sending a message below will automatically reopen and mark it as Open.
                          <form onSubmit={(e) => handleReplySubmit(e, ticket.id)} className="mt-2.5 space-y-2.5">
                            <div className="flex gap-2">
                              <input 
                                type="text" 
                                required
                                value={replyText}
                                onChange={(e) => setReplyText(e.target.value)}
                                placeholder="Type a message to reopen and reply..." 
                                className="input-field flex-1 py-2 px-3 text-sm bg-white rounded-xl"
                              />
                              <button 
                                type="submit" 
                                disabled={replyingId === ticket.id}
                                className="btn-primary py-2 px-4 rounded-xl text-sm shrink-0"
                              >
                                {replyingId === ticket.id ? 'Reopening...' : 'Reopen & Send'}
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
                  )}
                </div>

                <div className="mt-4 pt-3 border-t border-surface-100 text-xs text-surface-400 font-medium">
                  Submitted on {new Date(ticket.createdAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default ContactSupport
