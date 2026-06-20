import urllib.request
import json

# Register Company
req = urllib.request.Request("http://localhost:8080/api/retail-companies", 
                             data=json.dumps({"name": "Test Company"}).encode('utf-8'),
                             headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as response:
    company = json.loads(response.read().decode('utf-8'))
    company_id = company.get('retailCompanyId', company.get('id'))
    print("Company registered:", company_id)

# Register User
req = urllib.request.Request("http://localhost:8080/api/auth/register", 
                             data=json.dumps({
                                 "email": "testretail@gmail.com",
                                 "rawPassword": "password",
                                 "username": "Test User",
                                 "actor": "RETAIL",
                                 "retailCompanyId": company_id,
                                 "beneficiaryInstitutionId": None
                             }).encode('utf-8'),
                             headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as response:
    user = json.loads(response.read().decode('utf-8'))
    print("User registered:", user)

# Login
req = urllib.request.Request("http://localhost:8080/api/auth/login", 
                             data=json.dumps({
                                 "email": "testretail@gmail.com",
                                 "rawPassword": "password"
                             }).encode('utf-8'),
                             headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req) as response:
    auth_data = json.loads(response.read().decode('utf-8'))
    token = auth_data['token']
    print("Token received:", token)

# Get Profile
req = urllib.request.Request("http://localhost:8080/api/auth/profile", 
                             headers={"Authorization": "Bearer " + token})
with urllib.request.urlopen(req) as response:
    profile = json.loads(response.read().decode('utf-8'))
    print("Profile:", profile)

