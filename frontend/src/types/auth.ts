export interface UserDto {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: UserDto
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

export interface LoginRequest {
  email: string
  password: string
}
