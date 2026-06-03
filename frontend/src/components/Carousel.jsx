import React, { useRef, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const Carousel = ({ items, renderItem, autoSlide = true, slideInterval = 4000 }) => {
  const scrollRef = useRef(null);
  const [isHovered, setIsHovered] = useState(false);

  // Auto-slide logic
  useEffect(() => {
    if (!autoSlide || isHovered) return;
    
    const interval = setInterval(() => {
      if (scrollRef.current) {
        const { scrollLeft, scrollWidth, clientWidth } = scrollRef.current;
        // If we reached the end, scroll back to start (infinite loop simulation)
        if (scrollLeft + clientWidth >= scrollWidth - 10) {
          scrollRef.current.scrollTo({ left: 0, behavior: 'smooth' });
        } else {
          // Scroll one item width. Assuming item width is roughly 280px + gap 24px = ~304px
          // A safer way is clientWidth / number_of_visible, but we can just use fixed width or 300px
          scrollRef.current.scrollBy({ left: 320, behavior: 'smooth' });
        }
      }
    }, slideInterval);
    
    return () => clearInterval(interval);
  }, [autoSlide, isHovered, slideInterval]);

  const scroll = (direction) => {
    if (scrollRef.current) {
      const scrollAmount = direction === 'left' ? -320 : 320;
      scrollRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  };

  return (
    <div 
      className="relative group/carousel"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Navigation Buttons */}
      <button 
        onClick={() => scroll('left')}
        className="absolute left-0 top-1/2 -translate-y-1/2 -ml-4 z-20 bg-dark-800/80 hover:bg-brand-500 text-white p-2 rounded-full backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all border border-gray-800 hidden md:block shadow-xl"
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      
      <button 
        onClick={() => scroll('right')}
        className="absolute right-0 top-1/2 -translate-y-1/2 -mr-4 z-20 bg-dark-800/80 hover:bg-brand-500 text-white p-2 rounded-full backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all border border-gray-800 hidden md:block shadow-xl"
      >
        <ChevronRight className="h-5 w-5" />
      </button>

      {/* Scrollable Container */}
      <div 
        ref={scrollRef}
        className="flex overflow-x-auto snap-x snap-mandatory gap-6 pb-6 pt-2 scrollbar-hide hide-scrollbar w-full"
        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
      >
        {items.map((item, idx) => (
          <div key={`${item.id || idx}-${idx}`} className="snap-start shrink-0 w-[280px]">
            {renderItem(item)}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Carousel;
