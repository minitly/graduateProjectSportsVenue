import { computed } from 'vue'

export const useNaiveTheme = () => {
  const themeOverrides = computed(() => ({
    common: {
      primaryColor: '#2563eb',
      primaryColorHover: '#1d4ed8',
      primaryColorPressed: '#1e40af',
      borderRadius: '14px'
    },
    Card: {
      borderRadius: '22px'
    },
    Modal: {
      borderRadius: '22px'
    },
    Button: {
      borderRadiusTiny: '10px',
      borderRadiusSmall: '12px',
      borderRadiusMedium: '12px',
      borderRadiusLarge: '14px'
    }
  }))

  return { themeOverrides }
}
