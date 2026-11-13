import { useState } from 'react'
import { NavLink, Route, Routes } from 'react-router-dom'
import testLogo from './test-logo.svg'

function Home() {
  const [count, setCount] = useState(0)

  return (
    <section className="panel">
      <div className="logo-wrap"><img src={testLogo} alt="لوگوی تست محلی" /></div>
      <p className="eyebrow">REACT + VITE SMOKE TEST</p>
      <h1>سلام! 👋</h1>
      <p className="muted">اگر این صفحه را می‌بینی، React و فایل‌های خروجی Vite بارگذاری شده‌اند.</p>
      <div className="counter">
        <span>شمارندهٔ تعاملی</span>
        <strong>{count}</strong>
        <div className="actions">
          <button onClick={() => setCount((value) => value + 1)}>افزایش +</button>
          <button className="secondary" onClick={() => setCount(0)}>بازنشانی</button>
        </div>
      </div>
      <ul className="checks">
        <li>✓ اجرای JavaScript Module</li>
        <li>✓ بارگذاری CSS محلی</li>
        <li>✓ بارگذاری تصویر SVG محلی</li>
        <li>✓ تغییر State در React</li>
      </ul>
    </section>
  )
}

function Details() {
  return (
    <section className="panel">
      <p className="eyebrow">ROUTING TEST</p>
      <h1>صفحهٔ دوم</h1>
      <p className="muted">این صفحه با React Router و HashRouter باز شده است.</p>
      <p className="route-value">مسیر فعلی: <code>/#/details</code></p>
    </section>
  )
}

export default function App() {
  return (
    <main className="app">
      <nav className="nav">
        <span className="brand">HTML Runner</span>
        <div>
          <NavLink end to="/">خانه</NavLink>
          <NavLink to="/details">تست مسیر</NavLink>
        </div>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/details" element={<Details />} />
      </Routes>
      <footer>آزمون سازگاری · بدون نیاز به اینترنت پس از Build</footer>
    </main>
  )
}
