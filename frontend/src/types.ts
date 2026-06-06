export type Role = 'PASSENGER' | 'DRIVER';
export type AvailabilityStatus = 'OFFLINE' | 'ONLINE' | 'BUSY';
export type RideStatus = 'REQUESTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface Vehicle {
  id: number;
  vehicleNumber: string;
  vehicleType: string;
  capacity: number;
}

export interface PassengerDetails {
  passengerProfileId: number;
  campusAddress?: string | null;
}

export interface DriverDetails {
  driverProfileId: number;
  licenseNumber: string;
  verificationDocument: string;
  verificationStatus: VerificationStatus;
  availabilityStatus: AvailabilityStatus;
  averageRating: number;
  ratingCount: number;
  vehicle: Vehicle | null;
}

export interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: Role;
  createdAt: string;
  passenger: PassengerDetails | null;
  driver: DriverDetails | null;
}

export interface BasicUser {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: Role;
}

export interface DriverSummary {
  id: number;
  name: string;
  phone: string;
  availabilityStatus: AvailabilityStatus;
  averageRating: number;
  ratingCount: number;
  vehicle: Vehicle | null;
}

export interface Rating {
  id: number;
  rideId: number;
  passengerId: number;
  driverId: number;
  score: number;
  feedback?: string | null;
  createdAt: string;
}

export interface Ride {
  id: number;
  passenger: BasicUser;
  driver: BasicUser | null;
  pickupLocation: string;
  destination: string;
  status: RideStatus;
  requestedAt: string;
  acceptedAt?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  rating?: Rating | null;
}

export interface StatusCount {
  status: RideStatus;
  count: number;
}

export interface DriverDashboard {
  totalRidesCompleted: number;
  activeRides: number;
  averageRating: number;
  ratingCount: number;
  activeRide: Ride | null;
  rideHistory: Ride[];
  ratingsReceived: Rating[];
  rideStatusBreakdown: StatusCount[];
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface RealtimeEvent<T = unknown> {
  type: string;
  message: string;
  payload: T;
  timestamp: string;
}
