import React from 'react'

const Footer = () => {
  return (
    <footer className="bg-white border-t border-surface-200 mt-auto py-8">
      <div className="max-w-7xl mx-auto px-4 text-center text-sm text-surface-500 font-medium">
        <p>&copy; {new Date().getFullYear()} VortexShop. Designed for portfolio showcasing and enterprise software engineering demonstrations.</p>
        <p className="mt-2 text-xs text-surface-400">Built using Java 21, Spring Boot 3, MySQL, and React + Tailwind CSS.</p>
      </div>
    </footer>
  )
}

export default Footer
