import urllib
import urllib.parse
from flask import jsonify
from flask import request
from app.utils import create_presigned_url, upload_image_to_s3, valid_image_extension
from flask_jwt_extended import jwt_required, get_jwt_identity
from werkzeug.utils import secure_filename
from app import db, app, s3_client, bucket_name
from app.models import Image, Collection, User, images_collections

@app.route('/collections/<collection_id>/images/<image_id>', methods=['POST'])
@jwt_required()
def edit_image_in_collection(collection_id, image_id):
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()
    user_id = user.id
    image = request.files['image']

    new_s3_url = upload_image_to_s3(image, user_id)

    new_image = Image(user_id=user_id, s3_url=new_s3_url, filename=image.filename, filesize=image.content_length)
    db.session.add(new_image)
    db.session.commit()

    db.session.execute(
        images_collections.update().\
            where(db.and_(images_collections.c.collection_id == collection_id,
                          images_collections.c.image_id == image_id)).\
            values(image_id=new_image.id)
    )
    db.session.commit()
    presigned_url = create_presigned_url(bucket_name, urllib.parse.urlparse(new_s3_url).path[1:])
    return jsonify({'message': presigned_url}), 200

@app.route('/collections/<collection_id>', methods=['DELETE'])
@jwt_required()
def delete_collection(collection_id):
    collection = Collection.query.get(collection_id)
    if collection:
        db.session.delete(collection)
        db.session.commit()
        return jsonify({'message': f'Collection {collection_id} has been deleted.'}), 200
    else:
        return jsonify({'error': 'Collection not found.'}), 404

@app.route('/collections/<collection_id>/images/<image_id>', methods=['DELETE'])
@jwt_required()
def delete_image(collection_id, image_id):
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()
    user_id = user.id

    image = Image.query.get(image_id)
    collection = Collection.query.get(collection_id)

    if image is None or collection is None:
        return jsonify(error="Image or Collection not found"), 404

    collection.images.remove(image)

    s3_key = f'user_{user_id}/{secure_filename(image.filename)}'
    s3_client.delete_object(Bucket=bucket_name, Key=s3_key)

    db.session.delete(image)
    db.session.commit()

    return jsonify(message="Image deleted successfully"), 200

@app.route('/move_image', methods=['POST'])
def move_image():
    image_id = request.json.get('image_id')
    current_collection_id = request.json.get('current_collection_id')
    new_collection_id = request.json.get('new_collection_id')

    image = Image.query.get(image_id)
    current_collection = Collection.query.get(current_collection_id)
    new_collection = Collection.query.get(new_collection_id)

    if image not in current_collection.images:
        return jsonify({'error': 'Image not in current collection'}), 400

    current_collection.images.remove(image)
    db.session.commit()

    new_collection.images.append(image)
    db.session.commit()

    return jsonify({'image': image.serialize(), "message" : "Image uploaded successfully!"}), 200
@app.route('/collections/<collection_id>/images', methods=['GET'])
@jwt_required()
def get_collection_images(collection_id):
    images = Image.query.filter(Image.collections.any(id=collection_id)).all()
    return jsonify([image.serialize() for image in images]), 200

@app.route('/collections/<collection_id>/images', methods=['POST'])
@jwt_required()
def add_image_to_collection(collection_id):
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()

    if user is None:
        return jsonify({'message': 'User not found'}), 404

    if 'image' not in request.files:
        return jsonify({'message': 'No image provided'}), 400
    image = request.files['image']

    print(image.filename)
    if not valid_image_extension(image.filename):
        return jsonify({'message': 'Invalid image format'}), 400

    if collection_id is not None:
        collection = Collection.query.get(collection_id)
        if collection is None:
            return jsonify(error="Collection not found"), 404
    else:
        collection = Collection(user_id=user.id, name="New Collection")
        db.session.add(collection)
        db.session.commit()

    collection = Collection.query.get(collection_id)
    if collection.user_id != user.id:
        return jsonify({'message':
            'You do not have permission to add images to this collection'}), 403

    s3_url = upload_image_to_s3(image, user.id)

    new_image = Image(user_id=user.id,
                      s3_url=s3_url,
                      filename=image.filename,
                      filesize=image.content_length)
    db.session.add(new_image)
    db.session.commit()

    collection.images.append(new_image)
    db.session.commit()
    return jsonify({'image': new_image.serialize(),
                'message': "Image uploaded successfully!"}), 200

@app.route('/users/collections', methods=['GET'])
@jwt_required()
def get_user_collections():
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()

    if user is None:
        return jsonify({'error': 'User not found'}), 404

    requested_user_id = int(user.id)
    if user.id != requested_user_id:
        return jsonify({'error': 'Permission denied'}), 403

    collections = Collection.query.filter_by(user_id=user.id).all()

    if not collections:
        return jsonify({'message': 'No collections found for this user'}), 200

    collections_data = [{
        'id': collection.id,
        'name': collection.name,
        'image_count': len(collection.images),
        'images': [image.serialize() for image in Image.query.filter(Image.collections.any(id=collection.id)).all()] if collection.images else []
    } for collection in collections]

    return jsonify(collections_data), 200

@app.route('/users/collections', methods=['POST'])
@jwt_required()
def add_user_collection():
    user_email = get_jwt_identity()
    user = User.query.filter_by(email=user_email).first()

    if user is None:
        return jsonify({'error': 'User not found'}), 404

    data = request.get_json()
    collection_name = data.get('name')

    if not collection_name:
        return jsonify({'error': 'Collection name is required'}), 400

    new_collection = Collection(name=collection_name, user_id=user.id)
    db.session.add(new_collection)
    db.session.commit()

    return jsonify({'id': new_collection.id, 'name': new_collection.name}), 200
