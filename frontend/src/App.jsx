
import { useEffect, useRef, useState } from 'react'
import { apiUrl } from './api'
import AuthModal from './AuthModal'
import { DISTRICTS } from './districts'
import './App.css'

const posterByTitle = {
  interstellar:
    'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg',
  inception:
    'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
  'the dark knight':
    'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg',
}

function getPosterUrl(movie) {
  if (movie.posterPath) return `https://image.tmdb.org/t/p/w500${movie.posterPath}`

  return posterByTitle[(movie.title || '').trim().toLowerCase()] || ''
}

const WELCOME_COUPON_KEY = 'cinequeue.welcomeCouponNotice'
const WELCOME_COUPON_CLOSED_KEY = 'cinequeue.welcomeCouponClosed'

function welcomeCouponNotice() {
  try {
    const saved = JSON.parse(localStorage.getItem(WELCOME_COUPON_KEY) || '{}')
    return saved && typeof saved === 'object' ? saved : {}
  } catch {
    return {}
  }
}

function welcomeCouponEmailKey(email) {
  return String(email || '').trim().toLowerCase()
}

function App() {
  const [movies, setMovies] = useState([])
  const [heroIndex, setHeroIndex] = useState(0)
  const [heroPaused, setHeroPaused] = useState(false)
  const heroRailRef = useRef(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedMovie, setSelectedMovie] = useState(null)

  // Seat selection state
  const [selectedShowtime, setSelectedShowtime] = useState(null)
  const [seats, setSeats] = useState([])
  const [selectedSeatIds, setSelectedSeatIds] = useState([])
  const [seatLoading, setSeatLoading] = useState(false)
  const [seatError, setSeatError] = useState('')

  // Authentication state
  const [isAuthOpen, setIsAuthOpen] = useState(false)
  const [currentUser, setCurrentUser] = useState(null)
  const [accessToken, setAccessToken] = useState('')
  const [authNotice, setAuthNotice] = useState('')
  const [isBooking, setIsBooking] = useState(false)
  const [bookingResult, setBookingResult] = useState(null)
  const [showMyPage, setShowMyPage] = useState(false)
  const [myBookings, setMyBookings] = useState([])
  const [bookingsLoading, setBookingsLoading] = useState(false)
  const [bookingsError, setBookingsError] = useState('')
  const [cancellingBookingId, setCancellingBookingId] = useState(null)
  const [confirmCancelId, setConfirmCancelId] = useState(null)
  const [seatHold, setSeatHold] = useState(null)
  const [holdSecondsLeft, setHoldSecondsLeft] = useState(0)
  const [isPaying, setIsPaying] = useState(false)
  const [coupons, setCoupons] = useState([])
  const [selectedCouponId, setSelectedCouponId] = useState('')
  const [profileForm, setProfileForm] = useState({ name: '', ageGroup: 'ADULT', region: 'SEOUL', district: '용산구', preferredSeat: 'NO_PREFERENCE' })
  const [nearbyTheaters, setNearbyTheaters] = useState([])
  const [nearbyLoading, setNearbyLoading] = useState(true)
  const [nearbyError, setNearbyError] = useState('')
  const [selectedNearbyTheater, setSelectedNearbyTheater] = useState(null)
  const [theaterRegion, setTheaterRegion] = useState('ALL')
  const [theaterQuery, setTheaterQuery] = useState('')
  const [nearbyShowtimes, setNearbyShowtimes] = useState([])
  const [nearbyShowtimeLoading, setNearbyShowtimeLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [scheduleShowtimes, setScheduleShowtimes] = useState([])
  const [scheduleLoading, setScheduleLoading] = useState(true)
  const [scheduleError, setScheduleError] = useState('')
  const [selectedScheduleDate, setSelectedScheduleDate] = useState('')
  const [welcomeCouponEmail, setWelcomeCouponEmail] = useState('')

  // Load movies when the page opens
  useEffect(() => {
    const fetchMovies = async () => {
      try {
        const response = await fetch(apiUrl('/api/movies'))

        if (!response.ok) {
          throw new Error(`Request failed: ${response.status}`)
        }

        const data = await response.json()
        setMovies(data)
      } catch (err) {
        console.error('Failed to fetch movies:', err)
        setError('영화 목록을 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }

    fetchMovies()
  }, [])

  useEffect(() => {
    const region = currentUser?.region || 'SEOUL'
    const district = currentUser?.district || DISTRICTS[region][0]
    const fetchNearby = async () => {
      setNearbyLoading(true)
      setNearbyError('')
      try {
        const response = await fetch(apiUrl(`/api/theaters/nearby?region=${region}&district=${encodeURIComponent(district)}`))
        if (!response.ok) throw new Error(`Request failed: ${response.status}`)
        const data = await response.json()
        setNearbyTheaters(Array.isArray(data) ? data : [])
      } catch (err) {
        console.error('Failed to fetch theaters:', err)
        setNearbyError('가까운 영화관을 불러오지 못했습니다.')
      } finally {
        setNearbyLoading(false)
      }
    }
    fetchNearby()
  }, [currentUser?.region, currentUser?.district])

  useEffect(() => {
    if (currentUser?.region) setTheaterRegion(currentUser.region)
  }, [currentUser?.region])

  useEffect(() => {
    const fetchSchedule = async () => {
      setScheduleLoading(true)
      setScheduleError('')
      try {
        const response = await fetch(apiUrl('/api/showtimes'))
        if (!response.ok) throw new Error(`Request failed: ${response.status}`)
        const data = await response.json()
        setScheduleShowtimes(Array.isArray(data) ? data : [])
      } catch (err) {
        console.error('Failed to fetch schedule:', err)
        setScheduleError('상영 시간을 불러오지 못했습니다.')
      } finally {
        setScheduleLoading(false)
      }
    }

    fetchSchedule()
  }, [])

  // Restore the login state when the page is refreshed.
  useEffect(() => {
    let cancelled = false

    const restoreLogin = async () => {
      const savedToken = sessionStorage.getItem('accessToken')

      if (!savedToken) return

      try {
        const response = await fetch(apiUrl('/api/users/me'), {
          headers: {
            Authorization: `Bearer ${savedToken}`,
          },
        })

        if (!response.ok) {
          throw new Error('Login session expired')
        }

        const user = await response.json()

        if (!cancelled) {
          setAccessToken(savedToken)
          setCurrentUser(user)
          showPendingWelcomeCoupon(user?.email)
        }
      } catch (err) {
        sessionStorage.removeItem('accessToken')

        if (!cancelled) {
          setAccessToken('')
          setCurrentUser(null)
        }
      }
    }

    restoreLogin()

    return () => {
      cancelled = true
    }
  }, [])

  const showPendingWelcomeCoupon = (email) => {
    const key = welcomeCouponEmailKey(email)
    if (!key || welcomeCouponNotice()[key] !== 'pending') return
    if (sessionStorage.getItem(WELCOME_COUPON_CLOSED_KEY) === key) return
    setWelcomeCouponEmail(key)
  }

  const handleSignupSuccess = (email) => {
    const key = welcomeCouponEmailKey(email)
    if (!key || welcomeCouponNotice()[key] === 'dismissed') return
    const notice = welcomeCouponNotice()
    notice[key] = 'pending'
    localStorage.setItem(WELCOME_COUPON_KEY, JSON.stringify(notice))
    sessionStorage.removeItem(WELCOME_COUPON_CLOSED_KEY)
    setWelcomeCouponEmail(key)
  }

  const closeWelcomeCoupon = () => {
    if (welcomeCouponEmail) {
      sessionStorage.setItem(WELCOME_COUPON_CLOSED_KEY, welcomeCouponEmail)
    }
    setWelcomeCouponEmail('')
  }

  const dismissWelcomeCoupon = () => {
    if (welcomeCouponEmail) {
      const notice = welcomeCouponNotice()
      notice[welcomeCouponEmail] = 'dismissed'
      localStorage.setItem(WELCOME_COUPON_KEY, JSON.stringify(notice))
      sessionStorage.setItem(WELCOME_COUPON_CLOSED_KEY, welcomeCouponEmail)
    }
    setWelcomeCouponEmail('')
  }

  const handleLoginSuccess = ({ accessToken: token, user }) => {
    setAccessToken(token)
    setCurrentUser(user)
    setAuthNotice('로그인되었습니다.')
    showPendingWelcomeCoupon(user?.email)
  }

  const handleLogout = () => {
    sessionStorage.removeItem('accessToken')
    setAccessToken('')
    setCurrentUser(null)
    setMyBookings([])
    setShowMyPage(false)
    setBookingResult(null)
    setSeatHold(null)
    setCoupons([])
    setSelectedCouponId('')
    setAuthNotice('로그아웃되었습니다.')
  }

  const fetchCoupons = async () => {
    if (!accessToken) return
    try {
      const response = await fetch(apiUrl('/api/users/me/coupons'), {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) return
      setCoupons(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to fetch coupons:', err)
    }
  }

  const fetchMyBookings = async () => {
    if (!accessToken) return
    setBookingsLoading(true)
    setBookingsError('')
    try {
      const response = await fetch(apiUrl('/api/bookings'), {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) throw new Error(data?.message || data?.error || '예매 내역을 불러오지 못했습니다.')
      setMyBookings(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to fetch bookings:', err)
      setBookingsError(err.message || '예매 내역을 불러오지 못했습니다.')
    } finally {
      setBookingsLoading(false)
    }
  }

  const openMyPage = async () => {
    if (!accessToken || !currentUser) {
      setAuthNotice('마이페이지를 이용하려면 로그인해 주세요.')
      setIsAuthOpen(true)
      return
    }
    setShowMyPage(true)
    setBookingResult(null)
    setProfileError('')
    setProfileForm({
      name: currentUser.name || '',
      ageGroup: currentUser.ageGroup || 'ADULT',
      region: currentUser.region || 'SEOUL',
      district: currentUser.district || DISTRICTS[currentUser.region || 'SEOUL'][0],
      preferredSeat: currentUser.preferredSeat || 'NO_PREFERENCE',
    })
    await fetchMyBookings()
    await fetchCoupons()
    window.setTimeout(() => document.getElementById('my-page')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50)
  }

  const goToSection = (event, sectionId) => {
    if (!showMyPage) return
    event.preventDefault()
    setShowMyPage(false)
    const targetId = sectionId === 'showtimes' ? 'daily-schedule' : sectionId
    window.setTimeout(() => document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50)
  }

  const handleProfileChange = (event) => {
    const { name, value } = event.target
    setProfileForm((form) => {
      const next = { ...form, [name]: value }
      if (name === 'region' && !(DISTRICTS[value] || []).includes(form.district)) {
        next.district = DISTRICTS[value][0]
      }
      return next
    })
  }

  const handleProfileSave = async (event) => {
    event.preventDefault()
    if (profileSaving) return
    setProfileSaving(true)
    setProfileError('')
    try {
      const response = await fetch(apiUrl('/api/users/me'), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(profileForm),
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) throw new Error(data?.message || data?.error || '프로필을 저장하지 못했습니다.')
      setCurrentUser(data)
      setAuthNotice('프로필이 저장되었습니다.')
    } catch (err) {
      console.error('Profile update failed:', err)
      setProfileError(err.message || '프로필을 저장하지 못했습니다.')
    } finally {
      setProfileSaving(false)
    }
  }

  const refreshCurrentUser = async () => {
    if (!accessToken) return
    const response = await fetch(apiUrl('/api/users/me'), {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!response.ok) return
    const user = await response.json()
    setCurrentUser(user)
  }

  const handleCancelBooking = async (bookingId) => {
    setConfirmCancelId(null)
    setCancellingBookingId(bookingId)
    setBookingsError('')
    try {
      const response = await fetch(apiUrl(`/api/bookings/${bookingId}/cancel`), {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) throw new Error(data?.message || data?.error || '예매 취소에 실패했습니다.')
      setAuthNotice(`예매 번호 ${bookingId}의 취소가 완료되었습니다.`)
      await refreshCurrentUser()
      await fetchMyBookings()
      if (selectedShowtime?.id) {
        const seatsResponse = await fetch(apiUrl(`/api/showtimes/${selectedShowtime.id}/seats`))
        if (seatsResponse.ok) {
          const updatedSeats = await seatsResponse.json()
          if (Array.isArray(updatedSeats)) setSeats(updatedSeats)
        }
      }
    } catch (err) {
      console.error('Cancel booking failed:', err)
      setBookingsError(err.message || '예매 취소에 실패했습니다.')
    } finally {
      setCancellingBookingId(null)
    }
  }

  const handleBookingContinue = async () => {
    if (selectedSeatIds.length === 0 || !selectedShowtime || isBooking) return
    if (!accessToken || !currentUser) {
      setAuthNotice('예매하려면 먼저 로그인해 주세요.')
      setIsAuthOpen(true)
      return
    }
    setIsBooking(true)
    setAuthNotice('좌석을 선점하고 있습니다...')
    try {
      const response = await fetch(apiUrl('/api/bookings'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify({ showtimeId: selectedShowtime.id, seatIds: selectedSeatIds }),
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          sessionStorage.removeItem('accessToken')
          setAccessToken('')
          setCurrentUser(null)
          setAuthNotice('로그인이 만료되었습니다. 다시 로그인해 주세요.')
          setIsAuthOpen(true)
          return
        }
        throw new Error(data?.message || data?.error || `선점에 실패했습니다. (${response.status})`)
      }
      setSeatHold({
        ...data,
        bookingId: data?.bookingId ?? data?.id,
        seatId: data?.seatId ?? selectedSeatIds[0],
        seatIds: data?.seatIds || selectedSeatIds,
        showtimeId: data?.showtimeId ?? selectedShowtime.id,
        movieTitle: data?.movieTitle || selectedMovie?.title,
        seatLabel: data?.seatLabel || selectedSeats.map((seat) => `${seat.seatRow}${seat.seatNumber}`).join(', '),
        startTime: data?.startTime || selectedShowtime.startTime,
        price: data?.price ?? priceForViewer(selectedShowtime) * selectedSeatIds.length,
        expiresAt: data?.expiresAt,
        status: data?.status || 'HOLD',
      })
      setSelectedSeatIds([])
      setAuthNotice('좌석을 3분간 선점했습니다. 시간 안에 결제해 주세요.')
      setSelectedCouponId('')
      await fetchCoupons()
      const seatsResponse = await fetch(apiUrl(`/api/showtimes/${selectedShowtime.id}/seats`))
      if (seatsResponse.ok) {
        const updatedSeats = await seatsResponse.json()
        if (Array.isArray(updatedSeats)) setSeats(updatedSeats)
      }
    } catch (err) {
      console.error('Seat hold failed:', err)
      setAuthNotice(err.message || '좌석 선점 중 오류가 발생했습니다.')
    } finally {
      setIsBooking(false)
    }
  }

  const reloadShowtimeSeats = async (showtimeId) => {
    if (!showtimeId) return
    const seatsResponse = await fetch(apiUrl(`/api/showtimes/${showtimeId}/seats`))
    if (!seatsResponse.ok) return
    const updatedSeats = await seatsResponse.json()
    if (Array.isArray(updatedSeats)) setSeats(updatedSeats)
  }

  const handlePayHold = async () => {
    if (!seatHold?.bookingId || isPaying) return
    setIsPaying(true)
    try {
      const response = await fetch(apiUrl(`/api/bookings/${seatHold.bookingId}/pay`), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(selectedCouponId ? { couponId: Number(selectedCouponId) } : {}),
      })
      const data = await response.json().catch(() => null)
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          sessionStorage.removeItem('accessToken')
          setAccessToken('')
          setCurrentUser(null)
          setSeatHold(null)
          setAuthNotice('로그인이 만료되었습니다. 다시 로그인해 주세요.')
          setIsAuthOpen(true)
          return
        }
        if (data?.message?.includes('선점')) setSeatHold(null)
        throw new Error(data?.message || data?.error || '결제에 실패했습니다.')
      }
      const confirmedBooking = {
        ...seatHold,
        ...data,
        status: data?.status || 'CONFIRMED',
      }
      setBookingResult(confirmedBooking)
      setSeatHold(null)
      setSelectedCouponId('')
      setShowMyPage(false)
      setAuthNotice(`예매가 완료되었습니다. 예매 번호: ${confirmedBooking.bookingId ?? '확인 불가'}`)
      await refreshCurrentUser()
      await reloadShowtimeSeats(data?.showtimeId || seatHold.showtimeId)
      window.setTimeout(() => document.getElementById('booking-confirmation')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50)
    } catch (err) {
      console.error('Payment failed:', err)
      setAuthNotice(err.message || '결제 중 오류가 발생했습니다.')
    } finally {
      setIsPaying(false)
    }
  }

  const handleReleaseHold = async () => {
    if (!seatHold?.bookingId || isPaying) return
    setIsPaying(true)
    try {
      const response = await fetch(apiUrl(`/api/bookings/${seatHold.bookingId}/hold`), {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      const data = await response.json().catch(() => null)
      if (!response.ok && response.status !== 409) {
        throw new Error(data?.message || data?.error || '선점 해제에 실패했습니다.')
      }
      const showtimeId = seatHold.showtimeId
      setSeatHold(null)
      setAuthNotice('좌석 선점을 해제했습니다.')
      await reloadShowtimeSeats(showtimeId)
    } catch (err) {
      console.error('Hold release failed:', err)
      setAuthNotice(err.message || '선점 해제 중 오류가 발생했습니다.')
    } finally {
      setIsPaying(false)
    }
  }

  useEffect(() => {
    if (!seatHold?.expiresAt) {
      setHoldSecondsLeft(0)
      return
    }

    let notified = false
    const tick = () => {
      const left = Math.max(0, Math.ceil((new Date(seatHold.expiresAt).getTime() - Date.now()) / 1000))
      setHoldSecondsLeft(left)
      if (left > 0 || notified) return
      notified = true
      const showtimeId = seatHold.showtimeId
      setSeatHold(null)
      setSelectedSeatIds([])
      setAuthNotice('선점 시간이 지나 좌석이 해제되었습니다.')
      reloadShowtimeSeats(showtimeId)
    }

    tick()
    const timerId = window.setInterval(tick, 1000)
    return () => window.clearInterval(timerId)
  }, [seatHold])

  // Scroll to the selected section after React renders it
  useEffect(() => {
    if (selectedShowtime) {
      document.getElementById('seats')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    }
  }, [selectedShowtime])

  // Load seats for the selected showtime
  const handleShowtimeSelect = async (showtime) => {
    setSelectedShowtime(showtime)
    setSeats([])
    setSelectedSeatIds([])
    setSeatError('')
    setSeatLoading(true)

    try {
      const response = await fetch(
        apiUrl(`/api/showtimes/${showtime.id}/seats`)
      )

      if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`)
      }

      const data = await response.json()

      if (!Array.isArray(data)) {
        throw new Error('Unexpected seat response')
      }

      setSeats(data)
    } catch (err) {
      console.error('Failed to fetch seats:', err)
      setSeatError(
        '좌석 정보를 불러오지 못했습니다. 좌석 API 경로와 서버 상태를 확인해 주세요.'
      )
    } finally {
      setSeatLoading(false)
    }
  }

  const openScheduleShowtime = (showtime) => {
    const movie = movies.find((item) => item.id === showtime.movieId) || {
      id: showtime.movieId,
      title: showtime.movieTitle,
      posterPath: '',
      ageRating: '',
      runningTime: null,
      description: '',
    }
    setSelectedMovie(movie)
    handleShowtimeSelect(showtime)
  }

  const openNearbyTheater = async (theater) => {
    setSelectedNearbyTheater(theater)
    setNearbyShowtimes([])
    setNearbyShowtimeLoading(true)
    try {
      const response = await fetch(apiUrl(`/api/theaters/${theater.id}/showtimes`))
      if (!response.ok) throw new Error(`Request failed: ${response.status}`)
      const data = await response.json()
      setNearbyShowtimes(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to fetch theater showtimes:', err)
      setNearbyError('선택한 영화관의 상영 시간을 불러오지 못했습니다.')
    } finally {
      setNearbyShowtimeLoading(false)
    }
  }

  // Select one available seat, or deselect it
  const handleSeatClick = (seat) => {
    if (seat.status !== 'AVAILABLE') return

    setSelectedSeatIds((currentIds) => {
      if (currentIds.includes(seat.id)) {
        return currentIds.filter((id) => id !== seat.id)
      }
      if (currentIds.length >= 10) {
        setAuthNotice('좌석은 한 번에 10개까지 선택할 수 있습니다.')
        return currentIds
      }
      return [...currentIds, seat.id]
    })
  }

  const closeSeatSelection = () => {
    setSelectedShowtime(null)
    setSeats([])
    setSelectedSeatIds([])
    setSeatError('')
  }

  const formatDateTime = (value) => {
    if (!value) return '시간 미정'

    return new Date(value).toLocaleString('ko-KR', {
      month: 'long',
      day: 'numeric',
      weekday: 'short',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  }

  const formatTime = (value) => {
    if (!value) return '--:--'

    return new Date(value).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
  }

  const formatScheduleDate = (dateKey) => {
    const date = new Date(`${dateKey}T00:00:00`)
    if (Number.isNaN(date.getTime())) return dateKey
    return date.toLocaleDateString('ko-KR', {
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    })
  }

  const formatPrice = (price) =>
    Number(price || 0).toLocaleString('ko-KR')

  const ageGroupLabel = {
    CHILD: '유아',
    TEEN: '청소년',
    ADULT: '성인',
  }

  const couponLabel = {
    WELCOME: '가입 환영',
    SILVER_REWARD: 'SILVER 달성',
    GOLD_REWARD: 'GOLD 달성',
    VIP_REWARD: 'VIP 달성',
  }

  const couponStatusLabel = {
    AVAILABLE: '사용 가능',
    USED: '사용 완료',
    EXPIRED: '만료',
  }

  const availableCoupons = coupons.filter((coupon) => coupon.status === 'AVAILABLE')
  const selectedCoupon = availableCoupons.find((coupon) => String(coupon.id) === String(selectedCouponId))
  const holdBasePrice = Number(seatHold?.price || 0)
  const holdDiscount = selectedCoupon ? Math.min(Number(selectedCoupon.discountAmount || 0), holdBasePrice) : 0
  const holdFinalPrice = Math.max(0, holdBasePrice - holdDiscount)

  const membershipGradeLabel = {
    BASIC: '일반',
    SILVER: 'SILVER',
    GOLD: 'GOLD',
    VIP: 'VIP',
  }

  const bookingsUntilNextGrade = (count) => {
    const confirmedCount = Number(count || 0)
    if (confirmedCount >= 20) return null
    if (confirmedCount >= 6) return 20 - confirmedCount
    if (confirmedCount >= 3) return 6 - confirmedCount
    return 3 - confirmedCount
  }

  const priceForViewer = (showtime) => {
    if (currentUser?.ageGroup === 'CHILD') return showtime.childPrice
    if (currentUser?.ageGroup === 'TEEN') return showtime.teenPrice
    return showtime.adultPrice ?? showtime.price
  }

  const heroMovies = movies.filter((movie) => getPosterUrl(movie))
  const featuredMovie = heroMovies[heroIndex] || movies[0] || null
  const featuredPoster = featuredMovie ? getPosterUrl(featuredMovie) : ''

  useEffect(() => {
    if (heroIndex >= heroMovies.length) {
      setHeroIndex(0)
    }
  }, [heroIndex, heroMovies.length])

  useEffect(() => {
    if (heroPaused || heroMovies.length < 2) return undefined

    const timer = window.setInterval(() => {
      setHeroIndex((index) => (index + 1) % heroMovies.length)
    }, 5000)

    return () => window.clearInterval(timer)
  }, [heroPaused, heroMovies.length])

  useEffect(() => {
    const track = heroRailRef.current
    const activeItem = track?.querySelector('.hero-rail-item.active')

    if (!track || !activeItem) return

    track.scrollTo({
      left:
        activeItem.offsetLeft -
        (track.clientWidth - activeItem.clientWidth) / 2,
      behavior: 'smooth',
    })
  }, [heroIndex])

  const showPreviousHero = () => {
    setHeroIndex((index) =>
      index === 0 ? heroMovies.length - 1 : index - 1
    )
  }

  const showNextHero = () => {
    setHeroIndex((index) => (index + 1) % heroMovies.length)
  }

  const selectedSeats = selectedSeatIds
    .map((seatId) => seats.find((seat) => seat.id === seatId))
    .filter(Boolean)
  const selectedSeat = selectedSeats[0] || null

  // Group seats by row, for example A1-A5 and B1-B5
  const seatRows = Object.values(
    seats.reduce((rows, seat) => {
      const rowName = seat.seatRow

      if (!rows[rowName]) {
        rows[rowName] = []
      }

      rows[rowName].push(seat)
      return rows
    }, {})
  ).sort((a, b) =>
    String(a[0]?.seatRow || '').localeCompare(
      String(b[0]?.seatRow || ''),
      'en'
    )
  )

  seatRows.forEach((row) => {
    row.sort((a, b) => a.seatNumber - b.seatNumber)
  })

  const availableSeatCount = seats.filter(
    (seat) => seat.status === 'AVAILABLE'
  ).length

  const reservedSeatCount = seats.filter(
    (seat) => seat.status === 'RESERVED'
  ).length

  const heldSeatCount = seats.filter(
    (seat) => seat.status === 'HOLD'
  ).length

  const holdTimeLabel = `${Math.floor(holdSecondsLeft / 60)}:${String(holdSecondsLeft % 60).padStart(2, '0')}`

  const scheduleGroups = (() => {
    const byDate = new Map()
    scheduleShowtimes.forEach((showtime) => {
      const dateKey = String(showtime.startTime || '').slice(0, 10)
      if (!dateKey) return
      if (!byDate.has(dateKey)) byDate.set(dateKey, new Map())
      const moviesOnDate = byDate.get(dateKey)
      if (!moviesOnDate.has(showtime.movieId)) {
        const movie = movies.find((item) => item.id === showtime.movieId)
        moviesOnDate.set(showtime.movieId, {
          movieId: showtime.movieId,
          title: showtime.movieTitle || movie?.title || '제목 미등록',
          posterPath: movie?.posterPath || '',
          ageRating: movie?.ageRating || '',
          runningTime: movie?.runningTime,
          showtimes: [],
        })
      }
      moviesOnDate.get(showtime.movieId).showtimes.push(showtime)
    })
    return [...byDate.entries()].map(([date, movieMap]) => ({
      date,
      movies: [...movieMap.values()],
    }))
  })()

  const activeScheduleDate = scheduleGroups.some((group) => group.date === selectedScheduleDate)
    ? selectedScheduleDate
    : scheduleGroups[0]?.date || ''
  const activeSchedule = scheduleGroups.find((group) => group.date === activeScheduleDate)
  const theaterKeyword = theaterQuery.trim().toLowerCase()
  const visibleTheaters = nearbyTheaters.filter((theater) => {
    if (theaterRegion !== 'ALL' && theater.region !== theaterRegion) return false
    if (!theaterKeyword) return true
    return `${theater.name} ${theater.district} ${theater.address}`.toLowerCase().includes(theaterKeyword)
  })

  return (
    <div className="app" id="top">
      <header className="navbar">
        <a
          className="logo"
          href="#top"
          onClick={closeSeatSelection}
          aria-label="CineQueue 홈"
        >
          <span className="logo-mark" aria-hidden="true">▶</span>
          <span>Cine<span>Queue</span></span>
        </a>

        <nav className="nav-links" aria-label="주요 메뉴">
          <a className="active" href="#top">홈</a>
          <a href="#movies" onClick={(event) => goToSection(event, 'movies')}>영화</a>
          <a href="#daily-schedule" onClick={(event) => goToSection(event, 'showtimes')}>상영 시간</a>
        </nav>

        <div className="nav-actions">
          <a className="nav-action" href="#movies" onClick={(event) => goToSection(event, 'movies')}>
            <span className="search-icon" aria-hidden="true">⌕</span>
            <span>영화 찾기</span>
          </a>

          {currentUser ? (
            <div className="nav-auth">
              <button type="button" className="nav-auth-button" onClick={openMyPage}>
                마이페이지
              </button>
              <span className="nav-user" title={currentUser.email}>
                {currentUser.email || '로그인 사용자'}
              </span>
              <button
                type="button"
                className="nav-auth-button"
                onClick={handleLogout}
              >
                로그아웃
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="nav-auth-button"
              onClick={() => {
                setAuthNotice('')
                setIsAuthOpen(true)
              }}
            >
              로그인
            </button>
          )}
        </div>
      </header>

      {!showMyPage && <main>
        <section
          className="hero"
          style={
            featuredPoster
              ? { '--hero-image': `url("${featuredPoster}")` }
              : undefined
          }
          onMouseEnter={() => setHeroPaused(true)}
          onMouseLeave={() => setHeroPaused(false)}
        >
          <div className="hero-overlay" />

          <div className="hero-content">
            <div className="hero-copy">
              <p className="hero-kicker">CINEQUEUE PRESENTS</p>

              {featuredMovie ? (
                <>
                  <h1>{featuredMovie.title}</h1>
                  <p className="hero-description">
                    {featuredMovie.description ||
                      '스크린에서 만나는 특별한 이야기.'}
                  </p>

                  <div className="hero-meta">
                    <span>{featuredMovie.ageRating || '등급 미정'}</span>
                    <span>
                      {featuredMovie.runningTime
                        ? `${featuredMovie.runningTime}분`
                        : '상영 시간 미정'}
                    </span>
                  </div>

                  <a className="primary-button hero-button" href="#daily-schedule">
                    상영 시간 확인하기
                    <span aria-hidden="true">→</span>
                  </a>
                </>
              ) : (
                <>
                  <h1>오늘, 영화 한 편 어떠세요?</h1>
                  <p className="hero-description">
                    보고 싶은 영화를 발견하고 나만의 시간을 예약하세요.
                  </p>
                  <a className="primary-button hero-button" href="#movies">
                    영화 둘러보기 <span aria-hidden="true">→</span>
                  </a>
                </>
              )}
            </div>

            {featuredPoster && (
              <img
                className="hero-poster"
                src={featuredPoster}
                alt=""
              />
            )}
          </div>

          {heroMovies.length > 1 && (
            <div className="hero-rail">
              <button
                type="button"
                className="hero-nav"
                onClick={showPreviousHero}
                aria-label="이전 영화"
              >
                ‹
              </button>
              <div className="hero-rail-track" ref={heroRailRef}>
                {heroMovies.map((movie, index) => (
                  <button
                    key={movie.id}
                    type="button"
                    className={`hero-rail-item ${
                      index === heroIndex ? 'active' : ''
                    }`}
                    onClick={() => setHeroIndex(index)}
                    aria-label={movie.title}
                    aria-current={index === heroIndex ? 'true' : undefined}
                  >
                    <img src={getPosterUrl(movie)} alt="" />
                  </button>
                ))}
              </div>
              <button
                type="button"
                className="hero-nav"
                onClick={showNextHero}
                aria-label="다음 영화"
              >
                ›
              </button>
            </div>
          )}

          <a className="hero-scroll" href="#movies">
            <span className="scroll-line" />
            <span>SCROLL TO EXPLORE</span>
          </a>
        </section>

        <section className="movie-section page-container" id="movies">
          <div className="section-heading">
            <div>
              <p className="section-kicker">NOW SHOWING</p>
              <h2>지금 상영 중인 영화</h2>
              <p className="section-description">
                오늘의 기분에 맞는 영화를 찾아보세요.
              </p>
            </div>

            <span className="movie-count">
              {String(movies.length).padStart(2, '0')} FILMS
            </span>
          </div>

          {loading && (
            <p className="message" role="status">
              영화 목록을 불러오는 중입니다...
            </p>
          )}

          {error && (
            <div className="message error" role="alert">
              <p>{error}</p>
              <button
                className="secondary-button"
                onClick={() => window.location.reload()}
              >
                다시 시도하기
              </button>
            </div>
          )}

          {!loading && !error && movies.length === 0 && (
            <p className="message">
              현재 등록된 영화가 없습니다.
            </p>
          )}

          {!loading && !error && movies.length > 0 && (
            <div className="movie-grid">
              {movies.map((movie) => {
                const posterUrl = getPosterUrl(movie)

                return (
                  <article className="movie-card" key={movie.id}>
                    <div className="poster-button">
                      <div className="movie-poster">
                        {posterUrl ? (
                          <img
                            className="poster-image"
                            src={posterUrl}
                            alt={`${movie.title} 영화 포스터`}
                            loading="lazy"
                            onError={(event) => {
                              event.currentTarget.style.display = 'none'
                            }}
                          />
                        ) : (
                          <div className="poster-placeholder">
                            <span className="placeholder-mark">▶</span>
                            <span>CINEQUEUE</span>
                          </div>
                        )}

                        <span className="poster-overlay" />
                        <span className="poster-label">NOW SHOWING</span>
                      </div>
                    </div>

                    <div className="movie-info">
                      <div className="movie-meta">
                        <span className="age-rating">
                          {movie.ageRating || '등급 미정'}
                        </span>
                        <span>
                          {movie.runningTime
                            ? `${movie.runningTime}분`
                            : '시간 미정'}
                        </span>
                      </div>

                      <h3>{movie.title || '제목 미등록'}</h3>

                      <p className="movie-description">
                        {movie.description ||
                          '영화 소개가 아직 등록되지 않았습니다.'}
                      </p>
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </section>

        <section id="nearby-theaters" className="nearby-theaters page-container">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Near you</p>
              <h2>가까운 영화관</h2>
            </div>
            <span>{currentUser?.district || DISTRICTS[currentUser?.region || 'SEOUL'][0]} 기준</span>
          </div>
          <div className="nearby-filters">
            <select
              aria-label="영화관 지역"
              value={theaterRegion}
              onChange={(event) => setTheaterRegion(event.target.value)}
            >
              <option value="ALL">전체 지역</option>
              <option value="SEOUL">서울</option>
              <option value="GYEONGGI">경기</option>
              <option value="INCHEON">인천</option>
              <option value="GANGWON">강원</option>
              <option value="CHUNGCHEONG">충청</option>
              <option value="JEOLLA">전라</option>
              <option value="GYEONGSANG">경상</option>
              <option value="JEJU">제주</option>
              <option value="OTHER">기타</option>
            </select>
            <input
              type="search"
              aria-label="영화관 검색"
              placeholder="영화관 이름 또는 주소"
              value={theaterQuery}
              onChange={(event) => setTheaterQuery(event.target.value)}
            />
          </div>
          {nearbyLoading && <p className="status-text">영화관을 불러오는 중...</p>}
          {nearbyError && <p className="status-text error-text">{nearbyError}</p>}
          {!nearbyLoading && !nearbyError && visibleTheaters.length === 0 && (
            <p className="status-text">조건에 맞는 영화관이 없습니다.</p>
          )}
          {!nearbyLoading && !nearbyError && visibleTheaters.length > 0 && (
            <div className="nearby-list">
              {visibleTheaters.map((theater) => (
                <button
                  key={theater.id}
                  type="button"
                  className={`nearby-card ${selectedNearbyTheater?.id === theater.id ? 'active' : ''}`}
                  onClick={() => openNearbyTheater(theater)}
                >
                  <strong>{theater.name}</strong>
                  <span>{theater.district} · {theater.address}</span>
                  <em>{theater.distanceKm}km</em>
                </button>
              ))}
            </div>
          )}
          {selectedNearbyTheater && visibleTheaters.some((theater) => theater.id === selectedNearbyTheater.id) && (
            <div className="nearby-showtimes">
              <h3>{selectedNearbyTheater.name} 상영 시간</h3>
              {nearbyShowtimeLoading && <p className="status-text">상영 시간을 불러오는 중...</p>}
              {!nearbyShowtimeLoading && nearbyShowtimes.length === 0 && (
                <p className="status-text">등록된 상영이 없습니다.</p>
              )}
              <div className="schedule-times">
                {nearbyShowtimes.map((showtime) => (
                  <button key={showtime.id} type="button" className="schedule-time" onClick={() => openScheduleShowtime(showtime)}>
                    <strong>{showtime.movieTitle}</strong>
                    <span>{formatTime(showtime.startTime)}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </section>

        <section id="daily-schedule" className="daily-schedule page-container">
          <div className="section-heading">
            <div>
              <p className="section-kicker">DAILY SCHEDULE</p>
              <h2>상영 시간</h2>
              <p className="section-description">날짜별로 영화, 상영관, 시간을 확인하고 좌석을 선택하세요.</p>
            </div>
          </div>

          {scheduleLoading && <p className="message" role="status">상영 시간을 불러오는 중입니다...</p>}
          {scheduleError && <p className="message error" role="alert">{scheduleError}</p>}
          {!scheduleLoading && !scheduleError && scheduleGroups.length === 0 && (
            <p className="message">등록된 상영 시간이 없습니다.</p>
          )}

          {!scheduleLoading && !scheduleError && scheduleGroups.length > 0 && (
            <>
              <div className="schedule-dates" role="tablist" aria-label="상영 날짜">
                {scheduleGroups.map((group) => (
                  <button
                    type="button"
                    key={group.date}
                    className={`schedule-date ${group.date === activeScheduleDate ? 'active' : ''}`}
                    onClick={() => setSelectedScheduleDate(group.date)}
                    aria-selected={group.date === activeScheduleDate}
                  >
                    {formatScheduleDate(group.date)}
                  </button>
                ))}
              </div>

              <div className="schedule-movies">
                {activeSchedule?.movies.map((movie) => {
                  const poster = getPosterUrl(movie)
                  return (
                    <article className="schedule-movie" key={movie.movieId}>
                      {poster ? (
                        <img className="schedule-poster" src={poster} alt="" />
                      ) : (
                        <div className="schedule-poster" aria-hidden="true" />
                      )}
                      <div className="schedule-movie-info">
                        <h3>{movie.title}</h3>
                        <div className="movie-meta">
                          <span className="age-rating">{movie.ageRating || '등급 미정'}</span>
                          <span>{movie.runningTime ? `${movie.runningTime}분` : '시간 미정'}</span>
                        </div>
                      </div>
                      <div className="schedule-times">
                        {movie.showtimes.map((showtime) => (
                          <button
                            type="button"
                            key={showtime.id}
                            className={`schedule-time ${selectedShowtime?.id === showtime.id ? 'active' : ''}`}
                            onClick={() => openScheduleShowtime(showtime)}
                          >
                            <strong>{showtime.theaterNumber}관</strong>
                            <span>{formatTime(showtime.startTime)}</span>
                          </button>
                        ))}
                      </div>
                    </article>
                  )
                })}
              </div>
            </>
          )}
        </section>

        {selectedShowtime && (
          <section
            className="seat-section page-container"
            id="seats"
            aria-live="polite"
          >
            <div className="seat-section-heading">
              <div>
                <p className="section-kicker">PICK YOUR SEAT</p>
                <h2>좌석 선택</h2>
                <p className="section-description">
                  원하는 좌석을 최대 10개까지 선택해 주세요.
                </p>
                {selectedShowtime.referenceLayout && (
                  <p className="seat-reference-note">
                    극장 좌석 수에 맞춘 배치입니다. 좌석 번호와 비어 있는 자리는 실제 상영관과 다릅니다.
                  </p>
                )}
              </div>

              <button
                className="back-button"
                onClick={closeSeatSelection}
              >
                상영 시간으로 돌아가기
              </button>
            </div>

            <div className="seat-panel">
              <div className="seat-main">
                <div className="screen-area">
                  <div className="screen-shape" />
                  <p>SCREEN</p>
                  <span>스크린 방향</span>
                </div>

                <div className="seat-legend">
                  <div>
                    <span className="legend-seat available" />
                    <span>선택 가능</span>
                  </div>
                  <div>
                    <span className="legend-seat selected" />
                    <span>내가 선택</span>
                  </div>
                  <div>
                    <span className="legend-seat held" />
                    <span>선점 중</span>
                  </div>
                  <div>
                    <span className="legend-seat reserved" />
                    <span>예매 완료</span>
                  </div>
                </div>

                {seatLoading && (
                  <p className="message" role="status">
                    좌석 정보를 불러오는 중입니다...
                  </p>
                )}

                {seatError && (
                  <div className="message error" role="alert">
                    <p>{seatError}</p>
                    <button
                      className="secondary-button"
                      onClick={() => handleShowtimeSelect(selectedShowtime)}
                    >
                      다시 시도하기
                    </button>
                  </div>
                )}

                {!seatLoading &&
                  !seatError &&
                  seats.length === 0 && (
                    <p className="message">
                      이 상영 회차에 등록된 좌석이 없습니다.
                    </p>
                  )}

                {!seatLoading &&
                  !seatError &&
                  seats.length > 0 && (
                    <>
                      <div className="seat-map">
                        {seatRows.map((row) => (
                          <div
                            className="seat-row"
                            key={row[0].seatRow}
                          >
                            <span className="row-label">
                              {row[0].seatRow}
                            </span>

                            <div className="seat-row-items">
                              {row.map((seat) => {
                                const isAvailable =
                                  seat.status === 'AVAILABLE'
                                const isSelected =
                                  selectedSeatIds.includes(seat.id)
                                const isReserved =
                                  seat.status === 'RESERVED'
                                const isHeld = seat.status === 'HOLD'
                                const isMyHold = (seatHold?.seatIds || [seatHold?.seatId]).includes(seat.id)

                                const seatClass = [
                                  'seat',
                                  isAvailable ? 'available' : '',
                                  isReserved ? 'reserved' : '',
                                  isHeld && !isMyHold ? 'held' : '',
                                  isSelected || isMyHold ? 'selected' : '',
                                ]
                                  .filter(Boolean)
                                  .join(' ')

                                const seatLabel =
                                  seat.seatLabel ||
                                  `${seat.seatRow}${seat.seatNumber}`

                                return (
                                  <button
                                    type="button"
                                    key={seat.id}
                                    className={seatClass}
                                    disabled={!isAvailable}
                                    onClick={() => handleSeatClick(seat)}
                                    aria-pressed={isSelected}
                                    aria-label={`${seatLabel}, ${
                                      isMyHold
                                        ? '내가 선점함'
                                        : isSelected
                                          ? '선택됨'
                                          : isHeld
                                            ? '선점 중'
                                            : isAvailable
                                              ? '선택 가능'
                                              : '예매 완료 또는 선택 불가'
                                    }`}
                                    title={
                                      isMyHold
                                        ? '결제 중인 좌석입니다.'
                                        : isHeld
                                          ? '다른 사용자가 선점한 좌석입니다.'
                                          : isReserved
                                            ? '이미 예매된 좌석입니다.'
                                            : !isAvailable
                                              ? '선택할 수 없는 좌석입니다.'
                                              : seatLabel
                                    }
                                  >
                                    {seat.seatNumber}
                                  </button>
                                )
                              })}
                            </div>

                            <span className="row-label">
                              {row[0].seatRow}
                            </span>
                          </div>
                        ))}
                      </div>

                      <div className="seat-summary-count">
                        <span>
                          전체 <strong>{seats.length}</strong>석
                        </span>
                        <span>
                          선택 가능 <strong>{availableSeatCount}</strong>석
                        </span>
                        <span>
                          선점 중 <strong>{heldSeatCount}</strong>석
                        </span>
                        <span>
                          예매 완료 <strong>{reservedSeatCount}</strong>석
                        </span>
                      </div>
                    </>
                  )}
              </div>

              <aside className="seat-summary">
                <p className="section-kicker">BOOKING SUMMARY</p>
                <h3>예매 정보</h3>

                <div className="summary-movie">
                  {selectedMovie.title}
                </div>

                <div className="summary-line">
                  <span>상영 날짜</span>
                  <strong>
                    {formatDateTime(selectedShowtime.startTime)}
                  </strong>
                </div>

                <div className="summary-line">
                  <span>영화관</span>
                  <strong>{selectedShowtime.theaterName || `${selectedShowtime.theaterNumber}관`}</strong>
                </div>
                {selectedShowtime.theaterName && (
                  <div className="summary-line">
                    <span>상영관</span>
                    <strong>{selectedShowtime.theaterNumber}관</strong>
                  </div>
                )}

                <div className="summary-line">
                  <span>상영 시간</span>
                  <strong>
                    {formatTime(selectedShowtime.startTime)}
                  </strong>
                </div>

                <div className="summary-line">
                  <span>선택 좌석</span>
                  <strong>
                    {selectedSeats.length > 0
                      ? selectedSeats.map((seat) => seat.seatLabel || `${seat.seatRow}${seat.seatNumber}`).join(', ')
                      : '선택 안 함'}
                  </strong>
                </div>

                <div className="summary-line">
                  <span>인원</span>
                  <strong>{selectedSeats.length > 0 ? `${selectedSeats.length}명` : '0명'}</strong>
                </div>

                <div className="summary-total">
                  <span>예상 결제 금액</span>
                  <strong>
                    {selectedSeats.length > 0
                      ? `${formatPrice(priceForViewer(selectedShowtime) * selectedSeats.length)}원`
                      : '0원'}
                  </strong>
                </div>

                {seatHold ? (
                  <div className="seat-hold-panel">
                    <p>{seatHold.movieTitle} {seatHold.seatLabel} 좌석이 준비됐어요. 아래 시간 안에 결제를 완료해 주세요.</p>
                    <p className="seat-hold-timer">{holdTimeLabel}</p>
                    {availableCoupons.length > 0 && (
                      <label className="seat-hold-coupon">
                        할인 쿠폰
                        <select value={selectedCouponId} onChange={(event) => setSelectedCouponId(event.target.value)}>
                          <option value="">사용 안 함</option>
                          {availableCoupons.map((coupon) => (
                            <option key={coupon.id} value={coupon.id}>
                              {couponLabel[coupon.couponType] || '쿠폰'} {formatPrice(coupon.discountAmount)}원
                            </option>
                          ))}
                        </select>
                      </label>
                    )}
                    {selectedCoupon && (
                      <p className="seat-hold-price">할인 {formatPrice(holdDiscount)}원 · 결제 {formatPrice(holdFinalPrice)}원</p>
                    )}
                    <div className="seat-hold-actions">
                      <button type="button" className="back-button" onClick={handleReleaseHold} disabled={isPaying}>결제 전 취소</button>
                      <button type="button" className="primary-button" onClick={handlePayHold} disabled={isPaying || holdSecondsLeft <= 0}>{isPaying ? '결제 중...' : '결제하기'}</button>
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="primary-button summary-button"
                    disabled={selectedSeats.length === 0 || isBooking}
                    onClick={handleBookingContinue}
                  >
                    {isBooking ? '선점 중...' : currentUser ? '예매 진행하기' : '로그인하고 예매하기'}
                    <span aria-hidden="true">→</span>
                  </button>
                )}

                <p className="summary-notice">
                  좌석을 고르면 3분 동안 선점됩니다.
                  <br />
                  시간 안에 결제해야 예매가 확정됩니다.
                </p>
              </aside>
            </div>
          </section>
        )}
      </main>}

      {bookingResult && !showMyPage && (
        <section id="booking-confirmation" className="booking-extra-section page-container">
          <div className="booking-confirmation-card">
            <div className="booking-confirmation-icon" aria-hidden="true">✓</div>
            <p className="section-kicker">BOOKING COMPLETE</p>
            <h2>예매가 완료되었습니다.</h2>
            <p className="booking-confirmation-description">예매 정보가 정상적으로 접수되었습니다.</p>
            <div className="booking-confirmation-details">
              <div className="summary-line"><span>예매 번호</span><strong>{bookingResult.bookingId ?? '확인 불가'}</strong></div>
              <div className="summary-line"><span>영화</span><strong>{bookingResult.movieTitle || selectedMovie?.title || '-'}</strong></div>
              <div className="summary-line"><span>상영 일시</span><strong>{formatDateTime(bookingResult.startTime || selectedShowtime?.startTime)}</strong></div>
              <div className="summary-line"><span>좌석</span><strong>{bookingResult.seatLabel || '-'}</strong></div>
              <div className="summary-line"><span>결제 금액</span><strong>{formatPrice(bookingResult.price ?? selectedShowtime?.price)}원</strong></div>
              <div className="summary-line"><span>상태</span><strong>{bookingResult.status === 'CANCELLED' ? '취소됨' : '예매 완료'}</strong></div>
            </div>
            <div className="booking-confirmation-actions">
              <button type="button" className="primary-button" onClick={openMyPage}>예매 내역 확인하기 <span aria-hidden="true">→</span></button>
              <button type="button" className="back-button" onClick={() => { setBookingResult(null); document.getElementById('movies')?.scrollIntoView({ behavior: 'smooth' }) }}>영화 더 보기</button>
            </div>
          </div>
        </section>
      )}

      {showMyPage && (
        <main id="my-page" className="my-page-section page-container">
          <div className="section-heading">
            <div><p className="section-kicker">MY CINEQUEUE</p><h2>마이페이지</h2><p className="section-description">예매 내역을 확인하고 취소할 수 있습니다.</p></div>
            <button type="button" className="back-button" onClick={() => setShowMyPage(false)}>홈으로 돌아가기</button>
          </div>
          {(() => {
            const grade = currentUser?.membershipGrade || 'BASIC'
            const confirmedCount = Number(currentUser?.confirmedBookingCount || 0)
            const nextGrade = { BASIC: 'SILVER', SILVER: 'GOLD', GOLD: 'VIP' }[grade]
            const nextThreshold = { BASIC: 3, SILVER: 6, GOLD: 20 }[grade]
            const progress = nextThreshold ? Math.min(100, (confirmedCount / nextThreshold) * 100) : 100
            return (
              <div className="my-page-user">
                <div className="my-page-user-card">
                  <span>로그인 계정</span>
                  <strong title={currentUser?.email}>{currentUser?.email || '로그인 사용자'}</strong>
                </div>
                <div className="my-page-user-card">
                  <span>요금 구분</span>
                  <strong>{ageGroupLabel[currentUser?.ageGroup] || '성인'}</strong>
                </div>
                <div className="my-page-user-card">
                  <span>회원 등급</span>
                  <strong><em className={`grade-badge grade-${grade.toLowerCase()}`}>{membershipGradeLabel[grade] || '일반'}</em></strong>
                </div>
                <div className="my-page-user-card">
                  <span>다음 등급까지</span>
                  {nextGrade ? (
                    <>
                      <strong>{nextGrade}까지 {bookingsUntilNextGrade(confirmedCount)}회</strong>
                      <div className="grade-progress" aria-hidden="true"><div style={{ width: `${progress}%` }} /></div>
                      <small>{confirmedCount} / {nextThreshold}회</small>
                    </>
                  ) : (
                    <strong>최고 등급 달성</strong>
                  )}
                </div>
              </div>
            )
          })()}
          <form className="profile-edit" onSubmit={handleProfileSave}>
            <h3>내 프로필 수정</h3>
            <div className="profile-edit-grid">
              <label>
                이름
                <input name="name" value={profileForm.name} onChange={handleProfileChange} maxLength={50} required />
              </label>
              <label>
                나이대
                <select name="ageGroup" value={profileForm.ageGroup} onChange={handleProfileChange}>
                  <option value="CHILD">유아</option>
                  <option value="TEEN">청소년</option>
                  <option value="ADULT">성인</option>
                </select>
              </label>
              <label>
                지역
                <select name="region" value={profileForm.region} onChange={handleProfileChange}>
                  <option value="SEOUL">서울</option>
                  <option value="GYEONGGI">경기</option>
                  <option value="INCHEON">인천</option>
                  <option value="GANGWON">강원</option>
                  <option value="CHUNGCHEONG">충청</option>
                  <option value="JEOLLA">전라</option>
                  <option value="GYEONGSANG">경상</option>
                  <option value="JEJU">제주</option>
                  <option value="OTHER">기타</option>
                </select>
              </label>
              <label>
                시·군·구
                <select name="district" value={profileForm.district} onChange={handleProfileChange}>
                  {(DISTRICTS[profileForm.region] || []).map((district) => (
                    <option key={district} value={district}>{district}</option>
                  ))}
                </select>
              </label>
              <label>
                선호 좌석
                <select name="preferredSeat" value={profileForm.preferredSeat} onChange={handleProfileChange}>
                  <option value="NO_PREFERENCE">선호 없음</option>
                  <option value="FRONT">앞쪽</option>
                  <option value="MIDDLE">중간</option>
                  <option value="BACK">뒤쪽</option>
                  <option value="AISLE">통로</option>
                  <option value="CENTER">중앙</option>
                </select>
              </label>
            </div>
            {profileError && <p className="profile-edit-error" role="alert">{profileError}</p>}
            <div className="profile-edit-actions">
              <button type="submit" className="primary-button" disabled={profileSaving}>{profileSaving ? '저장 중...' : '프로필 저장'}</button>
            </div>
          </form>
          <section className="my-coupon-section">
            <h3>내 쿠폰</h3>
            {coupons.length === 0 && <p className="message">보유한 쿠폰이 없습니다.</p>}
            {coupons.length > 0 && (
              <div className="my-coupon-list">
                {coupons.map((coupon) => (
                  <article className="my-coupon-card" key={coupon.id}>
                    <strong>{formatPrice(coupon.discountAmount)}원</strong>
                    <div>
                      <p>{couponLabel[coupon.couponType] || '쿠폰'}</p>
                      <span>{couponStatusLabel[coupon.status] || coupon.status}</span>
                      <span>{formatDateTime(coupon.expiresAt)}까지</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
          {bookingsLoading && <p className="message">예매 내역을 불러오는 중입니다...</p>}
          {bookingsError && <div className="message error" role="alert"><p>{bookingsError}</p><button type="button" className="secondary-button" onClick={fetchMyBookings}>다시 시도하기</button></div>}
          {!bookingsLoading && !bookingsError && myBookings.length === 0 && <p className="message">아직 예매 내역이 없습니다.</p>}
          {!bookingsLoading && !bookingsError && myBookings.length > 0 && (
            <div className="my-booking-list">
              {myBookings.map((booking) => {
                const cancelled = booking.status === 'CANCELLED'
                return (
                  <article className="my-booking-card" key={booking.bookingId}>
                    {booking.posterPath ? (
                      <img className="my-booking-poster" src={`https://image.tmdb.org/t/p/w500${booking.posterPath}`} alt="" />
                    ) : (
                      <div className="my-booking-poster" aria-hidden="true" />
                    )}
                    <div className="my-booking-body">
                    <div className="my-booking-card-header"><span className="section-kicker">BOOKING #{booking.bookingId}</span><span className={`booking-status ${cancelled ? 'cancelled' : 'confirmed'}`}>{cancelled ? '취소 완료' : '예매 완료'}</span></div>
                    <h3>{booking.movieTitle || '영화 제목 정보 없음'}</h3>
                    <div className="my-booking-details">
                      <div className="summary-line"><span>상영 일시</span><strong>{formatDateTime(booking.startTime)}</strong></div>
                      <div className="summary-line"><span>좌석</span><strong>{booking.seatLabel || '-'}</strong></div>
                      <div className="summary-line"><span>금액</span><strong>{formatPrice(booking.price)}원</strong></div>
                      {booking.bookedAt && <div className="summary-line"><span>예매 일시</span><strong>{formatDateTime(booking.bookedAt)}</strong></div>}
                    </div>
                    {!cancelled && confirmCancelId !== booking.bookingId && (
                      <div className="my-booking-actions">
                        <button type="button" className="cancel-booking-button" disabled={cancellingBookingId === booking.bookingId} onClick={() => setConfirmCancelId(booking.bookingId)}>{cancellingBookingId === booking.bookingId ? '취소 처리 중...' : '예매 취소'}</button>
                      </div>
                    )}
                    {!cancelled && confirmCancelId === booking.bookingId && (
                      <div className="cancel-confirm" role="alertdialog" aria-label="예매 취소 확인">
                        <p>이 예매를 취소할까요? 취소하면 되돌릴 수 없습니다.</p>
                        <div className="cancel-confirm-actions">
                          <button type="button" className="back-button" onClick={() => setConfirmCancelId(null)}>돌아가기</button>
                          <button type="button" className="cancel-booking-button confirm" onClick={() => handleCancelBooking(booking.bookingId)}>취소하기</button>
                        </div>
                      </div>
                    )}
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </main>
      )}

      <footer className="footer">
        <a className="footer-logo" href="#top">
          <span>▶</span> CineQueue
        </a>

        <p>Find your seat. Enjoy your movie.</p>

        <div className="footer-links">
          <a href="#movies">영화</a>
          <a href="#daily-schedule" onClick={(event) => goToSection(event, 'showtimes')}>상영 시간</a>
        </div>

        <span className="footer-copy">© 2026 CineQueue</span>
      </footer>

      {authNotice && (
        <div className="auth-toast" role="status">
          <span>{authNotice}</span>
          <button
            type="button"
            onClick={() => setAuthNotice('')}
            aria-label="알림 닫기"
          >
            ×
          </button>
        </div>
      )}

      {isAuthOpen && (
        <AuthModal
          onClose={() => setIsAuthOpen(false)}
          onLogin={handleLoginSuccess}
          onSignup={handleSignupSuccess}
        />
      )}

      {welcomeCouponEmail && (
        <div className="coupon-popup-overlay">
          <section
            className="coupon-popup"
            role="dialog"
            aria-modal="true"
            aria-labelledby="coupon-popup-title"
          >
            <p className="coupon-popup-kicker">WELCOME COUPON</p>
            <h2 id="coupon-popup-title">가입 축하 쿠폰이 지급되었습니다</h2>
            <div className="coupon-ticket" aria-hidden="true">
              <div className="coupon-ticket-main">
                <span>가입 환영</span>
                <strong>1,000원</strong>
                <em>할인 쿠폰</em>
              </div>
              <div className="coupon-ticket-side">
                <span>CineQueue</span>
              </div>
            </div>
            <div className="coupon-popup-actions">
              <button type="button" className="coupon-popup-close" onClick={closeWelcomeCoupon}>
                닫기
              </button>
              <button type="button" className="coupon-popup-dismiss" onClick={dismissWelcomeCoupon}>
                다시 보지 않기
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export default App