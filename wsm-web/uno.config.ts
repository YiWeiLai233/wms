import { defineConfig, presetUno, presetAttributify, presetIcons, transformerDirectives } from 'unocss'

export default defineConfig({
  presets: [presetUno(), presetAttributify(), presetIcons()],
  transformers: [transformerDirectives()],
  shortcuts: {
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'flex-col': 'flex flex-col',
    'page-container': 'p-6 bg-gray-50 min-h-full',
    'card': 'bg-white rounded-lg shadow-sm p-6',
    'card-header': 'flex items-center justify-between mb-4',
  },
  theme: {
    colors: {
      primary: '#3b82f6',
      sidebar: '#1e293b',
      'sidebar-hover': '#334155',
      'sidebar-active': '#3b82f6',
    },
  },
})
