import { Client } from '@stomp/stompjs';
import {
  Activity,
  Bell,
  CalendarClock,
  CheckCircle2,
  CircleDot,
  Clock3,
  LogOut,
  MapPin,
  Navigation,
  ShieldCheck,
  Star,
  UserRound,
  XCircle
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, sessionStore } from './api';
import type {
  AuthResponse,
  AvailabilityStatus,
  DriverDashboard as DriverDashboardData,
  DriverSummary,
  RealtimeEvent,
  Ride,
  RideStatus,
  User
} from './types';

const campusLocations = [
  'Rajiv Bhawan',
  'Main Building',
  'Central Library',
  'Student Activity Centre',
  'Cautley Bhawan',
  'Ravindra Bhawan',
  'Jawahar Bhawan',
  'LBS Stadium',
  'Convocation Hall',
  'Hospital',
  'Main Gate',
  'Century Gate'
];

const activeStatuses: RideStatus[] = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'];

const statusMeta: Record<RideStatus, { label: string; tone: string }> = {
  REQUESTED: { label: 'Requested', tone: 'warning' },
  ACCEPTED: { label: 'Accepted', tone: 'info' },
  IN_PROGRESS: { label: 'In progress', tone: 'active' },
  COMPLETED: { label: 'Completed', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' }
};

const availabilityMeta: Record<AvailabilityStatus, { label: string; tone: string }> = {
  ONLINE: { label: 'Online', tone: 'success' },
  OFFLINE: { label: 'Offline', tone: 'muted' },
  BUSY: { label: 'Busy', tone: 'active' }
};

export default function App() {
  const [session, setSession] = useState<AuthResponse | null>(() => sessionStore.read());
  const [notice, setNotice] = useState('Ready for campus rides');
  const [realtimeTick, setRealtimeTick] = useState(0);
  const authVersionRef = useRef(0);

  const saveSession = useCallback((nextSession: AuthResponse) => {
    authVersionRef.current += 1;
    sessionStore.write(nextSession);
    setSession(nextSession);
  }, []);

  const refreshUser = useCallback(async () => {
    if (!session) return;
    const refreshVersion = authVersionRef.current;
    const user = await api.me(session.token);
    const storedSession = sessionStore.read();
    if (authVersionRef.current !== refreshVersion || storedSession?.token !== session.token) {
      return;
    }
    const nextSession = { ...session, user };
    sessionStore.write(nextSession);
    setSession(nextSession);
  }, [session]);

  const signOut = useCallback(() => {
    authVersionRef.current += 1;
    sessionStore.clear();
    setSession(null);
  }, []);

  useEffect(() => {
    if (!session) return;

    const client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL ?? `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
      reconnectDelay: 2500,
      debug: () => undefined,
      onConnect: () => {
        const handleMessage = (raw: { body: string }) => {
          try {
            const event = JSON.parse(raw.body) as RealtimeEvent;
            setNotice(event.message);
          } catch {
            setNotice('Live update received');
          }
          setRealtimeTick((value) => value + 1);
        };

        client.subscribe(`/topic/users/${session.user.id}/notifications`, handleMessage);
        client.subscribe('/topic/drivers/availability', handleMessage);
        if (session.user.role === 'DRIVER') {
          client.subscribe('/topic/rides/requests', handleMessage);
          client.subscribe(`/topic/drivers/${session.user.id}/dashboard`, handleMessage);
        }
      }
    });

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [session?.token, session?.user.id, session?.user.role]);

  if (!session) {
    return <AuthScreen onAuthenticated={saveSession} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">
            <Navigation size={24} />
          </div>
          <div>
            <strong>Campus Ride</strong>
            <span>IIT Roorkee e-rickshaw dispatch</span>
          </div>
        </div>

        <div className="sidebar-card">
          <span className="eyebrow">Signed in</span>
          <strong>{session.user.name}</strong>
          <span>{session.user.email}</span>
        </div>

        <div className="live-pill">
          <Bell size={16} />
          <span>{notice}</span>
        </div>

        <button
          className="ghost-button sidebar-action"
          type="button"
          onClick={signOut}
        >
          <LogOut size={18} />
          Sign out
        </button>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <span className="eyebrow">{session.user.role === 'DRIVER' ? 'Driver workspace' : 'Passenger workspace'}</span>
            <h1>{session.user.role === 'DRIVER' ? 'Driver Dashboard' : 'Passenger Dashboard'}</h1>
          </div>
          <RoleBadge user={session.user} />
        </header>

        {session.user.role === 'PASSENGER' ? (
          <PassengerDashboard
            token={session.token}
            user={session.user}
            realtimeTick={realtimeTick}
            onUserRefresh={refreshUser}
            onNotice={setNotice}
          />
        ) : (
          <DriverDashboard
            token={session.token}
            user={session.user}
            realtimeTick={realtimeTick}
            onUserRefresh={refreshUser}
            onNotice={setNotice}
          />
        )}
      </main>
    </div>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (session: AuthResponse) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [role, setRole] = useState<'PASSENGER' | 'DRIVER'>('PASSENGER');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setLoading(true);
    const form = new FormData(event.currentTarget);
    try {
      let response: AuthResponse;
      if (mode === 'login') {
        response = await api.login({
          email: String(form.get('email') ?? ''),
          password: String(form.get('password') ?? '')
        });
      } else if (role === 'PASSENGER') {
        response = await api.registerPassenger({
          name: String(form.get('name') ?? ''),
          email: String(form.get('email') ?? ''),
          password: String(form.get('password') ?? ''),
          phone: String(form.get('phone') ?? ''),
          campusAddress: String(form.get('campusAddress') ?? '')
        });
      } else {
        response = await api.registerDriver({
          name: String(form.get('name') ?? ''),
          email: String(form.get('email') ?? ''),
          password: String(form.get('password') ?? ''),
          phone: String(form.get('phone') ?? ''),
          licenseNumber: String(form.get('licenseNumber') ?? ''),
          verificationDocument: String(form.get('verificationDocument') ?? ''),
          vehicleNumber: String(form.get('vehicleNumber') ?? ''),
          vehicleType: String(form.get('vehicleType') ?? 'E-Rickshaw'),
          vehicleCapacity: Number(form.get('vehicleCapacity') ?? 4)
        });
      }
      onAuthenticated(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-hero">
        <div className="brand-block">
          <div className="brand-mark">
            <Navigation size={26} />
          </div>
          <div>
            <strong>Campus Ride</strong>
            <span>Centralized e-rickshaw coordination</span>
          </div>
        </div>
        <h1>Real-time ride management for campus mobility</h1>
        <p>
          Passenger requests, driver availability, ride states, dashboards, and feedback stay synchronized from one focused web app.
        </p>
        <div className="auth-highlights">
          <span><CircleDot size={16} /> Live requests</span>
          <span><ShieldCheck size={16} /> Verified drivers</span>
          <span><Activity size={16} /> Driver insights</span>
        </div>
      </section>

      <section className="auth-card">
        <div className="segmented-control">
          <button className={mode === 'login' ? 'active' : ''} type="button" onClick={() => setMode('login')}>
            Login
          </button>
          <button className={mode === 'register' ? 'active' : ''} type="button" onClick={() => setMode('register')}>
            Register
          </button>
        </div>

        {mode === 'register' && (
          <div className="role-switch">
            <button className={role === 'PASSENGER' ? 'selected' : ''} type="button" onClick={() => setRole('PASSENGER')}>
              Passenger
            </button>
            <button className={role === 'DRIVER' ? 'selected' : ''} type="button" onClick={() => setRole('DRIVER')}>
              Driver
            </button>
          </div>
        )}

        <form className="form-grid" onSubmit={submit}>
          {mode === 'register' && (
            <>
              <label>
                Name
                <input name="name" placeholder="Aarav Singh" required />
              </label>
              <label>
                Phone
                <input name="phone" placeholder="+91 98765 43210" required />
              </label>
            </>
          )}

          <label>
            Email
            <input name="email" type="email" placeholder="you@iitr.ac.in" required />
          </label>
          <label>
            Password
            <input name="password" type="password" minLength={6} placeholder="Minimum 6 characters" required />
          </label>

          {mode === 'register' && role === 'PASSENGER' && (
            <label className="span-two">
              Campus address
              <input name="campusAddress" placeholder="Hostel, department, or staff quarter" />
            </label>
          )}

          {mode === 'register' && role === 'DRIVER' && (
            <>
              <label>
                License number
                <input name="licenseNumber" placeholder="DL-UK-2026-1234" required />
              </label>
              <label>
                Verification info
                <input name="verificationDocument" placeholder="Aadhaar / campus permit reference" required />
              </label>
              <label>
                Vehicle number
                <input name="vehicleNumber" placeholder="UK 08 ER 1204" required />
              </label>
              <label>
                Vehicle type
                <input name="vehicleType" defaultValue="E-Rickshaw" required />
              </label>
              <label>
                Capacity
                <input name="vehicleCapacity" type="number" min={1} defaultValue={4} required />
              </label>
            </>
          )}

          {error && <p className="form-error span-two">{error}</p>}

          <button className="primary-button span-two" type="submit" disabled={loading}>
            {loading ? 'Please wait...' : mode === 'login' ? 'Login' : `Create ${role.toLowerCase()} account`}
          </button>
        </form>
      </section>
    </main>
  );
}

function PassengerDashboard({
  token,
  user,
  realtimeTick,
  onUserRefresh,
  onNotice
}: {
  token: string;
  user: User;
  realtimeTick: number;
  onUserRefresh: () => Promise<void>;
  onNotice: (notice: string) => void;
}) {
  const [drivers, setDrivers] = useState<DriverSummary[]>([]);
  const [rides, setRides] = useState<Ride[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pickupLocation, setPickupLocation] = useState(campusLocations[0]);
  const [destination, setDestination] = useState(campusLocations[1]);

  const load = useCallback(async () => {
    setError('');
    try {
      const [availableDrivers, myRides] = await Promise.all([api.availableDrivers(token), api.myRides(token)]);
      setDrivers(availableDrivers);
      setRides(myRides);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load passenger dashboard');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load, realtimeTick]);

  const activeRide = useMemo(() => rides.find((ride) => activeStatuses.includes(ride.status)) ?? null, [rides]);
  const completedRides = rides.filter((ride) => ride.status === 'COMPLETED');

  async function requestRide(event: React.FormEvent) {
    event.preventDefault();
    setError('');
    try {
      await api.createRide(token, { pickupLocation, destination });
      onNotice('Ride requested');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not request ride');
    }
  }

  async function cancelRide(rideId: number) {
    setError('');
    try {
      await api.cancelRide(token, rideId);
      onNotice('Ride cancelled');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not cancel ride');
    }
  }

  async function submitRating(rideId: number, score: number, feedback: string) {
    setError('');
    try {
      await api.rateRide(token, { rideId, rating: score, feedback });
      onNotice('Feedback submitted');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not submit rating');
    }
  }

  return (
    <div className="dashboard-grid">
      <section className="panel span-two">
        <PanelHeader icon={<MapPin size={20} />} title="Request a ride" subtitle="Choose pickup and destination inside campus" />
        <form className="request-form" onSubmit={requestRide}>
          <LocationSelect label="Pickup" value={pickupLocation} onChange={setPickupLocation} />
          <LocationSelect label="Destination" value={destination} onChange={setDestination} />
          <button className="primary-button" type="submit" disabled={Boolean(activeRide)}>
            Request ride
          </button>
        </form>
        {activeRide && <p className="soft-note">Finish or cancel your active ride before requesting another one.</p>}
      </section>

      <section className="panel">
        <PanelHeader icon={<UserRound size={20} />} title="Available drivers" subtitle="Live online driver pool" />
        <div className="driver-list">
          {drivers.length === 0 ? (
            <EmptyState text="No drivers online right now" />
          ) : (
            drivers.map((driver) => <DriverRow key={driver.id} driver={driver} />)
          )}
        </div>
      </section>

      <ProfilePanel token={token} user={user} onUserRefresh={onUserRefresh} onNotice={onNotice} />

      <section className="panel span-two">
        <PanelHeader icon={<Clock3 size={20} />} title="Current ride" subtitle="Live ride lifecycle status" />
        {activeRide ? (
          <RideCard ride={activeRide} actions={<button className="danger-button" type="button" onClick={() => cancelRide(activeRide.id)}>Cancel ride</button>} />
        ) : (
          <EmptyState text="No active ride. Request one when you are ready." />
        )}
      </section>

      <section className="panel span-two">
        <PanelHeader icon={<Star size={20} />} title="Ride history and feedback" subtitle="Rate completed rides once" />
        {loading ? <EmptyState text="Loading rides..." /> : null}
        {error && <p className="form-error">{error}</p>}
        <div className="history-list">
          {rides.length === 0 && !loading ? <EmptyState text="No rides yet" /> : null}
          {rides.map((ride) => (
            <div className="history-item" key={ride.id}>
              <RideCard ride={ride} />
              {ride.status === 'COMPLETED' && !ride.rating && (
                <RatingForm rideId={ride.id} onSubmit={submitRating} />
              )}
            </div>
          ))}
        </div>
        <div className="mini-stats">
          <StatCard label="Total rides" value={rides.length} icon={<Activity size={18} />} />
          <StatCard label="Completed" value={completedRides.length} icon={<CheckCircle2 size={18} />} />
          <StatCard label="Rated rides" value={completedRides.filter((ride) => ride.rating).length} icon={<Star size={18} />} />
        </div>
      </section>
    </div>
  );
}

function DriverDashboard({
  token,
  user,
  realtimeTick,
  onUserRefresh,
  onNotice
}: {
  token: string;
  user: User;
  realtimeTick: number;
  onUserRefresh: () => Promise<void>;
  onNotice: (notice: string) => void;
}) {
  const [dashboard, setDashboard] = useState<DriverDashboardData | null>(null);
  const [incoming, setIncoming] = useState<Ride[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      const [dashboardData, requests] = await Promise.all([api.driverDashboard(token), api.incomingRequests(token)]);
      setDashboard(dashboardData);
      setIncoming(requests);
      await onUserRefresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load driver dashboard');
    }
  }, [token, onUserRefresh]);

  useEffect(() => {
    void load();
  }, [load, realtimeTick]);

  async function runAction(action: () => Promise<unknown>, success: string) {
    setBusy(true);
    setError('');
    try {
      await action();
      onNotice(success);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setBusy(false);
    }
  }

  const availability = user.driver?.availabilityStatus ?? 'OFFLINE';
  const chartData = dashboard?.rideStatusBreakdown.map((item) => ({
    status: statusMeta[item.status].label,
    count: item.count
  })) ?? [];

  return (
    <div className="dashboard-grid">
      <section className="panel span-two">
        <PanelHeader icon={<ShieldCheck size={20} />} title="Availability" subtitle="Control whether passengers and dispatch can see you" />
        <div className="availability-strip">
          <StatusPill label={availabilityMeta[availability].label} tone={availabilityMeta[availability].tone} />
          <button
            className={availability === 'OFFLINE' ? 'primary-button' : 'secondary-button'}
            type="button"
            disabled={busy || availability === 'BUSY'}
            onClick={() =>
              runAction(
                () => (availability === 'OFFLINE' ? api.goOnline(token) : api.goOffline(token)),
                availability === 'OFFLINE' ? 'You are online' : 'You are offline'
              )
            }
          >
            {availability === 'OFFLINE' ? 'Go online' : availability === 'BUSY' ? 'Ride in progress' : 'Go offline'}
          </button>
          {user.driver?.vehicle && (
            <span className="vehicle-chip">
              {user.driver.vehicle.vehicleNumber} · {user.driver.vehicle.vehicleType} · {user.driver.vehicle.capacity} seats
            </span>
          )}
        </div>
      </section>

      <ProfilePanel token={token} user={user} onUserRefresh={onUserRefresh} onNotice={onNotice} />

      <section className="panel">
        <PanelHeader icon={<Bell size={20} />} title="Incoming requests" subtitle="First accepted driver gets the ride" />
        {error && <p className="form-error">{error}</p>}
        <div className="request-list">
          {incoming.length === 0 ? (
            <EmptyState text="No pending ride requests" />
          ) : (
            incoming.map((ride) => (
              <RideCard
                key={ride.id}
                ride={ride}
                compact
                actions={
                  <div className="action-row">
                    <button className="primary-button" type="button" disabled={busy || availability !== 'ONLINE'} onClick={() => runAction(() => api.acceptRide(token, ride.id), 'Ride accepted')}>
                      Accept
                    </button>
                    <button className="ghost-button" type="button" disabled={busy} onClick={() => runAction(() => api.rejectRide(token, ride.id), 'Ride rejected')}>
                      Reject
                    </button>
                  </div>
                }
              />
            ))
          )}
        </div>
      </section>

      <section className="panel span-two">
        <PanelHeader icon={<Navigation size={20} />} title="Active ride" subtitle="Move the ride through its lifecycle" />
        {dashboard?.activeRide ? (
          <RideCard
            ride={dashboard.activeRide}
            actions={
              <div className="action-row">
                {dashboard.activeRide.status === 'ACCEPTED' && (
                  <button className="primary-button" type="button" disabled={busy} onClick={() => runAction(() => api.startRide(token, dashboard.activeRide!.id), 'Ride started')}>
                    Start ride
                  </button>
                )}
                {dashboard.activeRide.status === 'IN_PROGRESS' && (
                  <button className="primary-button" type="button" disabled={busy} onClick={() => runAction(() => api.completeRide(token, dashboard.activeRide!.id), 'Ride completed')}>
                    Complete
                  </button>
                )}
                <button className="danger-button" type="button" disabled={busy} onClick={() => runAction(() => api.cancelRide(token, dashboard.activeRide!.id), 'Ride cancelled')}>
                  Cancel
                </button>
              </div>
            }
          />
        ) : (
          <EmptyState text="No active ride assigned" />
        )}
      </section>

      <section className="panel span-two">
        <PanelHeader icon={<Activity size={20} />} title="Performance" subtitle="Completed rides, active rides, ratings, and history" />
        <div className="mini-stats">
          <StatCard label="Completed rides" value={dashboard?.totalRidesCompleted ?? 0} icon={<CheckCircle2 size={18} />} />
          <StatCard label="Active rides" value={dashboard?.activeRides ?? 0} icon={<CircleDot size={18} />} />
          <StatCard label="Average rating" value={(dashboard?.averageRating ?? 0).toFixed(2)} icon={<Star size={18} />} />
          <StatCard label="Ratings" value={dashboard?.ratingCount ?? 0} icon={<Bell size={18} />} />
        </div>
        <div className="chart-wrap">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="status" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#0f766e" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section className="panel span-two">
        <PanelHeader icon={<CalendarClock size={20} />} title="Ride history" subtitle="Recent assigned rides and passenger feedback" />
        <div className="history-list">
          {dashboard?.rideHistory.length === 0 ? <EmptyState text="No ride history yet" /> : null}
          {dashboard?.rideHistory.map((ride) => <RideCard key={ride.id} ride={ride} />)}
        </div>
      </section>
    </div>
  );
}

function ProfilePanel({
  token,
  user,
  onUserRefresh,
  onNotice
}: {
  token: string;
  user: User;
  onUserRefresh: () => Promise<void>;
  onNotice: (notice: string) => void;
}) {
  const [name, setName] = useState(user.name);
  const [phone, setPhone] = useState(user.phone);
  const [campusAddress, setCampusAddress] = useState(user.passenger?.campusAddress ?? '');
  const [error, setError] = useState('');

  useEffect(() => {
    setName(user.name);
    setPhone(user.phone);
    setCampusAddress(user.passenger?.campusAddress ?? '');
  }, [user]);

  async function save(event: React.FormEvent) {
    event.preventDefault();
    setError('');
    try {
      await api.updateMe(token, { name, phone, campusAddress });
      onNotice('Profile updated');
      await onUserRefresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update profile');
    }
  }

  return (
    <section className="panel">
      <PanelHeader icon={<UserRound size={20} />} title="Profile" subtitle="Account details" />
      <form className="profile-form" onSubmit={save}>
        <label>
          Name
          <input value={name} onChange={(event) => setName(event.target.value)} required />
        </label>
        <label>
          Phone
          <input value={phone} onChange={(event) => setPhone(event.target.value)} required />
        </label>
        {user.role === 'PASSENGER' && (
          <label>
            Campus address
            <input value={campusAddress} onChange={(event) => setCampusAddress(event.target.value)} />
          </label>
        )}
        {user.role === 'DRIVER' && user.driver && (
          <div className="driver-verification">
            <StatusPill label={user.driver.verificationStatus} tone="success" />
            <span>License: {user.driver.licenseNumber}</span>
          </div>
        )}
        {error && <p className="form-error">{error}</p>}
        <button className="secondary-button" type="submit">
          Save profile
        </button>
      </form>
    </section>
  );
}

function LocationSelect({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label>
      {label}
      <input list="campus-locations" value={value} onChange={(event) => onChange(event.target.value)} required />
      <datalist id="campus-locations">
        {campusLocations.map((location) => (
          <option key={location} value={location} />
        ))}
      </datalist>
    </label>
  );
}

function DriverRow({ driver }: { driver: DriverSummary }) {
  return (
    <div className="driver-row">
      <div className="avatar">{driver.name.slice(0, 1).toUpperCase()}</div>
      <div>
        <strong>{driver.name}</strong>
        <span>{driver.vehicle?.vehicleNumber ?? 'Vehicle pending'} · {driver.phone}</span>
      </div>
      <StatusPill label={`${Number(driver.averageRating).toFixed(1)} ★`} tone="success" />
    </div>
  );
}

function RideCard({ ride, actions, compact = false }: { ride: Ride; actions?: React.ReactNode; compact?: boolean }) {
  return (
    <article className={`ride-card ${compact ? 'compact' : ''}`}>
      <div className="ride-card-top">
        <div>
          <span className="eyebrow">Ride #{ride.id}</span>
          <strong>{ride.pickupLocation} → {ride.destination}</strong>
        </div>
        <StatusPill label={statusMeta[ride.status].label} tone={statusMeta[ride.status].tone} />
      </div>
      <div className="ride-meta-grid">
        <span><MapPin size={15} /> Pickup: {ride.pickupLocation}</span>
        <span><Navigation size={15} /> Destination: {ride.destination}</span>
        <span><Clock3 size={15} /> Requested: {formatTime(ride.requestedAt)}</span>
        <span><UserRound size={15} /> Passenger: {ride.passenger.name}</span>
        {ride.driver && <span><ShieldCheck size={15} /> Driver: {ride.driver.name}</span>}
        {ride.rating && <span><Star size={15} /> Rating: {ride.rating.score}/5</span>}
      </div>
      {actions && <div className="ride-actions">{actions}</div>}
    </article>
  );
}

function RatingForm({
  rideId,
  onSubmit
}: {
  rideId: number;
  onSubmit: (rideId: number, score: number, feedback: string) => Promise<void>;
}) {
  const [score, setScore] = useState(5);
  const [feedback, setFeedback] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    try {
      await onSubmit(rideId, score, feedback);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="rating-form" onSubmit={submit}>
      <label>
        Rating
        <select value={score} onChange={(event) => setScore(Number(event.target.value))}>
          {[5, 4, 3, 2, 1].map((value) => (
            <option key={value} value={value}>{value} stars</option>
          ))}
        </select>
      </label>
      <label>
        Feedback
        <input value={feedback} onChange={(event) => setFeedback(event.target.value)} placeholder="Optional feedback" />
      </label>
      <button className="secondary-button" type="submit" disabled={loading}>
        Submit feedback
      </button>
    </form>
  );
}

function RoleBadge({ user }: { user: User }) {
  if (user.role === 'DRIVER') {
    const availability = user.driver?.availabilityStatus ?? 'OFFLINE';
    return (
      <div className="role-badge">
        <ShieldCheck size={18} />
        <span>Driver</span>
        <StatusPill label={availabilityMeta[availability].label} tone={availabilityMeta[availability].tone} />
      </div>
    );
  }
  return (
    <div className="role-badge">
      <UserRound size={18} />
      <span>Passenger</span>
    </div>
  );
}

function PanelHeader({ icon, title, subtitle }: { icon: React.ReactNode; title: string; subtitle: string }) {
  return (
    <div className="panel-header">
      <div className="panel-icon">{icon}</div>
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
    </div>
  );
}

function StatCard({ label, value, icon }: { label: string; value: string | number; icon: React.ReactNode }) {
  return (
    <div className="stat-card">
      <span>{icon}</span>
      <strong>{value}</strong>
      <small>{label}</small>
    </div>
  );
}

function StatusPill({ label, tone }: { label: string; tone: string }) {
  return <span className={`status-pill ${tone}`}>{label}</span>;
}

function EmptyState({ text }: { text: string }) {
  return (
    <div className="empty-state">
      <XCircle size={18} />
      <span>{text}</span>
    </div>
  );
}

function formatTime(value?: string | null) {
  if (!value) return 'Not set';
  return new Intl.DateTimeFormat('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: 'short'
  }).format(new Date(value));
}
