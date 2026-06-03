export const getImageUrl = (url) => {
  if (!url) return 'https://via.placeholder.com/600x400?text=No+Image';
  if (url.startsWith('http')) return url;
  
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  const host = baseUrl.replace(/\/api\/?$/, '');
  
  return `${host}${url.startsWith('/') ? '' : '/'}${url}`;
}
