import { defineStore } from 'pinia'

export const useBookingsStore = defineStore('bookings', {
  state: () => ({
    items: [],
    loading: false,
  }),
  actions: {
    setBookings(bookings) {
      this.items = bookings
    },
  },
})
