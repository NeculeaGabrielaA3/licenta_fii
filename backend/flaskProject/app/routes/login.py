import re
from flask import jsonify, request
from flask_jwt_extended import jwt_required, create_access_token, get_jwt_identity, create_refresh_token
from werkzeug.security import generate_password_hash, check_password_hash
from app import db, app
from app.models import User
from app.utils import validate_password, validate_email

@app.route('/login', methods = ['POST'])
def login():
    json_data = request.json
    user_email = json_data['email']
    user_password = json_data['password']

    user = User.query.filter_by(email=user_email).first()

    if not user:
        return jsonify({'message': 'User does not exist.','access_token': None, 'refresh_token': None})

    user_id = user.id
    if check_password_hash(user.password, user_password):
        access_token = create_access_token(identity=user_email)
        return jsonify({'message':"Logged in successfully!", 'user_id': user_id, 'access_token': access_token}), 200
    else:
        return jsonify({'message': 'Incorrect password.', 'user_id': user_id})

@app.route('/signup', methods = ['POST'])
def signup():
    json_data = request.json
    user_email = json_data['email']
    user_password = json_data['password']
    user_first_name = json_data['first_name']
    user_last_name = json_data["last_name"]
    user_gender = json_data["gender"]
    user_username = json_data["username"]

    if not validate_email(user_email):
        return jsonify({'message': 'Invalid email address.'})

    if not validate_password(user_password):
        return jsonify({'message': 'Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.'})

    user = User.query.filter_by(email=user_email).first()
    if user:
        return jsonify({'message': 'User already exists.'})

    new_user = User(email=user_email, password=generate_password_hash(user_password, method='sha256'), first_name=user_first_name, last_name=user_last_name, gender=user_gender, username=user_username, profile_picture=None, bio=None)
    db.session.add(new_user)
    db.session.commit()

    response_data = {'message': 'User created successfully.'}
    return jsonify(response_data)

@app.route('/logout', methods=['GET'])
@jwt_required()
def logout():
    current_user = get_jwt_identity()
    if current_user:
        return jsonify(message=current_user), 200