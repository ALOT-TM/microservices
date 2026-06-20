const http = require('http');

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api' + path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length
      }
    }, res => {
      let resBody = '';
      res.on('data', chunk => resBody += chunk);
      res.on('end', () => {
        if (res.statusCode >= 400) return reject(`Error ${res.statusCode}: ${resBody}`);
        resolve(JSON.parse(resBody));
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function get(path, token) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api' + path,
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token
      }
    }, res => {
      let resBody = '';
      res.on('data', chunk => resBody += chunk);
      res.on('end', () => {
        if (res.statusCode >= 400) return reject(`Error ${res.statusCode}: ${resBody}`);
        resolve(JSON.parse(resBody));
      });
    });
    req.on('error', reject);
    req.end();
  });
}

async function run() {
  try {
    const company = await post('/retail-companies', { name: "Node Test Co" });
    const companyId = company.retailCompanyId || company.id;
    console.log("Company:", companyId);

    const user = await post('/auth/register', {
      email: "testnode@gmail.com",
      rawPassword: "password",
      username: "Node User",
      actor: "RETAIL",
      retailCompanyId: companyId,
      beneficiaryInstitutionId: null
    });
    console.log("User:", user);

    const auth = await post('/auth/login', {
      email: "testnode@gmail.com",
      rawPassword: "password"
    });
    console.log("Token:", auth.token);

    const profile = await get('/auth/profile', auth.token);
    console.log("Profile:", profile);
  } catch (e) {
    console.error(e);
  }
}
run();
