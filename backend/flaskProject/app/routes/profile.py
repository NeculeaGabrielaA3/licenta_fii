import urllib
import urllib.parse
from app import bucket_name
from app.utils import create_presigned_url
from flask import jsonify, request
from flask_jwt_extended import jwt_required, get_jwt_identity
from app import db, app
from app.models import User
from app.routes.gallery import upload_image_to_s3

@app.route('/users/profile-picture', methods=['PUT'])
@jwt_required()
def update_profile_picture():
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()

    profile_picture = request.files['image']
    s3_url = upload_image_to_s3(profile_picture, user.id)

    if not profile_picture:
        return jsonify({"message": 'No profile picture provided'}), 400

    user.update_profile_picture(s3_url)
    return jsonify({"message": 'Profile picture updated successfully'}), 200
@app.route('/user', methods=['GET'])
@jwt_required()
def get_user():
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()
    if user is None:
        return jsonify({"message": "User not found"}), 404

    profile_pic_url = create_presigned_url(bucket_name, urllib.parse.urlparse(user.profile_picture).path[1:])
    user_dict = {
        "id": user.id,
        "email": user.email,
        "first_name": user.first_name,
        "last_name": user.last_name,
        "username": user.username,
        "profile_picture": profile_pic_url,
        "bio": user.bio,
        "gender": user.gender,
        "images_count": len(user.images),
        "collections_count": len(user.collections)
    }
    return jsonify(user_dict)

@app.route('/user', methods=['DELETE'])
@jwt_required()
def delete_user():
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()
    if not user:
        return jsonify({"message": "User not found"}), 404

    db.session.delete(user)
    db.session.commit()

    return jsonify({"message": "User has been deleted successfully"}), 200