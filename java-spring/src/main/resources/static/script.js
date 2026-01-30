function addCredentials(form) {
  form.username.value = localStorage.getItem("username");
  form.password.value = localStorage.getItem("password");
}

function interact(target, endpoint) {
  const post_id = target.getAttribute('data-post_id');
  const username = localStorage.getItem("username");
  const password = localStorage.getItem("password");
  fetch(endpoint, {
       method: 'POST',
       headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
       body: `post_id=${encodeURIComponent(post_id)}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
     })
     .then(res => res.text())
     .then(text => {
       if (text.trim() === "false") {
         alert("Cannot delete other user's post.");
       }
     });
}

function likePost(event) {
  interact(event.currentTarget, '/posts/like');
  event.stopPropagation();
}

function dislikePost(event) {
  interact(event.currentTarget, '/posts/dislike');
  event.stopPropagation();
}

function deletePost(event) {
  interact(event.currentTarget, '/posts/delete');
  event.stopPropagation();
}

function drillInto(event) {
  window.location.href = `/posts?parent=${event.currentTarget.getAttribute('data-_id')}`;
  event.stopPropagation();
}

async function credentialsValid() {
  const username = localStorage.getItem("username");
  const password = localStorage.getItem("password");

  const res = await fetch("/authenticate", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({ username, password })
  });

  if (!res.ok || (await res.text()).trim() !== 'true') {
    return false;
  }

  return true;
}

function logout() {
  console.log("logging out");
  localStorage.clear();
  redirectToLogin(false);
}

async function redirectToLogin(checkCredentials) {
  if (!checkCredentials || !(await credentialsValid())) {
    localStorage.clear();
    window.location.href = '/';
  }
}

function redirectToPostsIfCredentialsPresent() {
  if (localStorage.getItem('username') !== null && localStorage.getItem('password') !== null) {
    window.location.href = '/posts'
  }
}
