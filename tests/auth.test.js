const request = require('supertest');

let app, server;

beforeAll(() => {
  process.env.JWT_SECRET = 'test-secret';
  process.env.PORT = '0';
  ({ app, server } = require('../src/index'));
});

afterAll((done) => {
  server.close(done);
});

const testUser = { email: 'test@example.com', password: 'password123', name: 'Test User' };

describe('Auth Service', () => {
  let accessToken;

  test('POST /api/v1/auth/register - creates user', async () => {
    const res = await request(app).post('/api/v1/auth/register').send(testUser);
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('accessToken');
    expect(res.body).toHaveProperty('refreshToken');
    expect(res.body.user.email).toBe(testUser.email);
  });

  test('POST /api/v1/auth/register - rejects duplicate', async () => {
    const res = await request(app).post('/api/v1/auth/register').send(testUser);
    expect(res.status).toBe(409);
  });

  test('POST /api/v1/auth/login - returns tokens', async () => {
    const res = await request(app).post('/api/v1/auth/login').send({ email: testUser.email, password: testUser.password });
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('accessToken');
    accessToken = res.body.accessToken;
  });

  test('POST /api/v1/auth/login - rejects bad password', async () => {
    const res = await request(app).post('/api/v1/auth/login').send({ email: testUser.email, password: 'wrong' });
    expect(res.status).toBe(401);
  });

  test('GET /api/v1/auth/me - returns user with valid token', async () => {
    const res = await request(app).get('/api/v1/auth/me').set('Authorization', `Bearer ${accessToken}`);
    expect(res.status).toBe(200);
    expect(res.body.email).toBe(testUser.email);
  });

  test('GET /api/v1/auth/me - rejects invalid token', async () => {
    const res = await request(app).get('/api/v1/auth/me').set('Authorization', 'Bearer invalid-token');
    expect(res.status).toBe(401);
  });

  test('GET /api/v1/auth/me - rejects missing token', async () => {
    const res = await request(app).get('/api/v1/auth/me');
    expect(res.status).toBe(401);
  });

  test('GET /health - returns ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});
