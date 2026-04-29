require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const Joi = require('joi');
const winston = require('winston');
const { authenticate } = require('./middleware/auth');

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(winston.format.timestamp(), winston.format.json()),
  transports: [new winston.transports.Console()],
});

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'change-me-in-production';
const JWT_EXPIRY = process.env.JWT_EXPIRY || '15m';
const REFRESH_EXPIRY = process.env.REFRESH_EXPIRY || '7d';

// In-memory store (replace with DB in production)
const users = new Map();
const refreshTokens = new Set();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10kb' }));

// Request logging
app.use((req, res, next) => {
  logger.info({ method: req.method, path: req.path });
  next();
});

// --- Health endpoints ---
app.get('/health', (_req, res) => res.json({ status: 'ok' }));
app.get('/ready', (_req, res) => res.json({ status: 'ready' }));

// --- Validation schemas ---
const registerSchema = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().min(8).required(),
  name: Joi.string().min(1).max(100).required(),
});

const loginSchema = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().required(),
});

// --- Helper ---
function generateTokens(user) {
  const payload = { sub: user.id, email: user.email };
  const accessToken = jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRY });
  const refreshToken = jwt.sign({ ...payload, type: 'refresh' }, JWT_SECRET, { expiresIn: REFRESH_EXPIRY });
  refreshTokens.add(refreshToken);
  return { accessToken, refreshToken };
}

// --- Routes ---
app.post('/api/v1/auth/register', async (req, res) => {
  try {
    const { error, value } = registerSchema.validate(req.body);
    if (error) return res.status(400).json({ error: error.details[0].message });

    if (users.has(value.email)) {
      return res.status(409).json({ error: 'Email already registered' });
    }

    const hashedPassword = await bcrypt.hash(value.password, 12);
    const user = { id: Date.now().toString(36), email: value.email, name: value.name, password: hashedPassword };
    users.set(value.email, user);

    const tokens = generateTokens(user);
    logger.info({ event: 'user_registered', email: user.email });
    res.status(201).json({ user: { id: user.id, email: user.email, name: user.name }, ...tokens });
  } catch (err) {
    logger.error({ event: 'register_error', error: err.message });
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/v1/auth/login', async (req, res) => {
  try {
    const { error, value } = loginSchema.validate(req.body);
    if (error) return res.status(400).json({ error: error.details[0].message });

    const user = users.get(value.email);
    if (!user || !(await bcrypt.compare(value.password, user.password))) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const tokens = generateTokens(user);
    logger.info({ event: 'user_login', email: user.email });
    res.json({ user: { id: user.id, email: user.email, name: user.name }, ...tokens });
  } catch (err) {
    logger.error({ event: 'login_error', error: err.message });
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.post('/api/v1/auth/refresh', (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken || !refreshTokens.has(refreshToken)) {
      return res.status(401).json({ error: 'Invalid refresh token' });
    }

    const decoded = jwt.verify(refreshToken, JWT_SECRET);
    if (decoded.type !== 'refresh') return res.status(401).json({ error: 'Invalid token type' });

    refreshTokens.delete(refreshToken);
    const user = { id: decoded.sub, email: decoded.email };
    const tokens = generateTokens(user);
    res.json(tokens);
  } catch (err) {
    res.status(401).json({ error: 'Invalid refresh token' });
  }
});

app.get('/api/v1/auth/me', authenticate, (req, res) => {
  const user = users.get(req.user.email);
  if (!user) return res.status(404).json({ error: 'User not found' });
  res.json({ id: user.id, email: user.email, name: user.name });
});

// 404 handler
app.use((_req, res) => res.status(404).json({ error: 'Not found' }));

// Error handler
app.use((err, _req, res, _next) => {
  logger.error({ event: 'unhandled_error', error: err.message });
  res.status(500).json({ error: 'Internal server error' });
});

const server = app.listen(PORT, () => {
  logger.info({ event: 'server_started', port: PORT });
});

// Graceful shutdown
const shutdown = (signal) => {
  logger.info({ event: 'shutdown', signal });
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10000);
};
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));

module.exports = { app, server };
